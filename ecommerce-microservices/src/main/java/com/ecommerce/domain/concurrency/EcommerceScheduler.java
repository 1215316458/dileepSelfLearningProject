package com.ecommerce.domain.concurrency;

import com.ecommerce.domain.model.Product;
import com.ecommerce.domain.repository.ProductRepository;
import com.ecommerce.domain.repository.concurrency.InventoryManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class EcommerceScheduler {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private final ProductRepository productRepo;
    private final InventoryManager inventoryManager;

    // CopyOnWriteArrayList — thread-safe for frequent reads, rare writes (active sessions)
    // Every write creates a new copy of the array — expensive for writes, free for reads
    private final CopyOnWriteArrayList<Long> activeSessions = new CopyOnWriteArrayList<>();

    private final AtomicInteger taskRunCount = new AtomicInteger(0);

    public EcommerceScheduler(ProductRepository productRepo, InventoryManager inventoryManager) {
        this.productRepo      = productRepo;
        this.inventoryManager = inventoryManager;
    }

    public void start() {
        // fixedRate — fires every 5s regardless of how long the task takes
        // If task takes longer than period, next run starts immediately after (no overlap by default)
        scheduler.scheduleAtFixedRate(this::checkLowStock, 0, 5, TimeUnit.SECONDS);

        // fixedDelay — waits 10s AFTER the previous task completes before firing again
        // Use fixedDelay when task duration is variable and you don't want overlap
        scheduler.scheduleWithFixedDelay(this::printOrderMetrics, 2, 10, TimeUnit.SECONDS);

        // one-shot after 30s — clean expired carts
        scheduler.scheduleAtFixedRate(this::cleanExpiredCarts, 5, 30, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) scheduler.shutdownNow();
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void checkLowStock() {
        taskRunCount.incrementAndGet();
        long lowStockCount = productRepo.findAll().stream()
                .filter(p -> p.getStock() > 0 && p.getStock() <= 5)
                .count();
        if (lowStockCount > 0) {
            System.out.println("  [Scheduler] LOW STOCK ALERT: " + lowStockCount + " products below threshold");
        }
    }

    private void printOrderMetrics() {
        System.out.println("  [Scheduler] Order metrics — active sessions: " + activeSessions.size());
    }

    private void cleanExpiredCarts() {
        System.out.println("  [Scheduler] Cleaning expired carts");
    }

    public void addSession(Long userId)    { activeSessions.add(userId); }
    public void removeSession(Long userId) { activeSessions.remove(userId); }
    public int getTaskRunCount()           { return taskRunCount.get(); }

    // --- ForkJoinPool: recursive inventory value calculation ---
    // RecursiveTask<BigDecimal> — splits the product list in half recursively
    // until the chunk is small enough to compute directly (threshold)
    // ForkJoinPool uses work-stealing: idle threads steal tasks from busy threads' queues
    public BigDecimal calculateInventoryValue(List<Product> products) {
        return ForkJoinPool.commonPool().invoke(new InventoryValueTask(products, 0, products.size()));
    }

    static class InventoryValueTask extends RecursiveTask<BigDecimal> {

        private static final int THRESHOLD = 10; // compute directly if <= 10 products
        private final List<Product> products;
        private final int start;
        private final int end;

        InventoryValueTask(List<Product> products, int start, int end) {
            this.products = products;
            this.start    = start;
            this.end      = end;
        }

        @Override
        protected BigDecimal compute() {
            if (end - start <= THRESHOLD) {
                // base case — compute directly
                BigDecimal sum = BigDecimal.ZERO;
                for (int i = start; i < end; i++) {
                    Product p = products.get(i);
                    sum = sum.add(p.getPrice().multiply(BigDecimal.valueOf(p.getStock())));
                }
                return sum;
            }

            // split in half — fork left half, compute right half on this thread
            int mid = (start + end) / 2;
            InventoryValueTask left  = new InventoryValueTask(products, start, mid);
            InventoryValueTask right = new InventoryValueTask(products, mid, end);

            left.fork();                          // submit left to ForkJoinPool asynchronously
            BigDecimal rightResult = right.compute(); // compute right on current thread
            BigDecimal leftResult  = left.join();     // wait for left to finish

            return leftResult.add(rightResult);
        }
    }
}
