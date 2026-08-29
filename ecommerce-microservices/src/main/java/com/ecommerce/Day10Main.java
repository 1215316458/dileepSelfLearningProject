package com.ecommerce;

import com.ecommerce.domain.enums.Category;
import com.ecommerce.domain.model.Product;
import com.ecommerce.domain.repository.ProductRepository;
import com.ecommerce.domain.repository.concurrency.InventoryManager;
import com.ecommerce.domain.repository.concurrency.OrderCounter;
import com.ecommerce.domain.repository.concurrency.OrderQueue;
import com.ecommerce.domain.repository.concurrency.RequestContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Day10Main {

    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
        testConcurrentHashMap();
        testInventoryManager();
        testOrderCounter();
        testRequestContext();
        testOrderQueue();
    }

    // --- checkpoint 1: ConcurrentHashMap in ProductRepository ---

    private static void testConcurrentHashMap() throws InterruptedException {
        System.out.println("=== ConcurrentHashMap in ProductRepository ===");

        ProductRepository repo = new ProductRepository();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(100);

        // 100 threads saving products concurrently — no corruption with ConcurrentHashMap
        for (int i = 0; i < 100; i++) {
            final long id = i + 1;
            executor.submit(() -> {
                try {
                    repo.save(new Product(id, "Product-" + id, "Desc",
                            new BigDecimal("10.00"), 5, Category.ELECTRONICS));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        System.out.println("  100 concurrent saves — count: " + repo.count()
                + (repo.count() == 100 ? " ✓" : " ← CORRUPTION"));
    }

    // --- checkpoint 2: InventoryManager with ReentrantReadWriteLock ---

    private static void testInventoryManager() throws InterruptedException {
        System.out.println("\n=== InventoryManager (ReentrantReadWriteLock) ===");

        InventoryManager manager = new InventoryManager();
        Product product = new Product(1L, "Laptop", "Desc", new BigDecimal("999.00"), 100, Category.ELECTRONICS);
        manager.addProduct(product);

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(110);

        // 100 reader threads — all run concurrently (readLock allows this)
        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                try {
                    int stock = manager.getStock(1L);
                    // just read — no print to avoid console flood
                } finally {
                    latch.countDown();
                }
            });
        }

        // 10 writer threads — each reserves 1 unit (writeLock serialises these)
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    manager.reserveStock(1L, 1);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        System.out.println("  Initial stock: 100");
        System.out.println("  After 10 reservations: " + manager.getStock(1L)
                + (manager.getStock(1L) == 90 ? " ✓" : " ← WRONG"));

        // release 5 back
        manager.releaseStock(1L, 5);
        System.out.println("  After releasing 5: " + manager.getStock(1L)
                + (manager.getStock(1L) == 95 ? " ✓" : " ← WRONG"));

        // checkAvailability
        System.out.println("  Available(50): " + manager.checkAvailability(1L, 50)  + " (expect true)");
        System.out.println("  Available(99): " + manager.checkAvailability(1L, 99)  + " (expect false)");
    }

    // --- checkpoint 3: OrderCounter with AtomicInteger ---

    private static void testOrderCounter() throws InterruptedException {
        System.out.println("\n=== OrderCounter (AtomicInteger) ===");

        OrderCounter counter = new OrderCounter();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(1000);

        // 1000 threads all incrementing — AtomicInteger CAS ensures no lost updates
        for (int i = 0; i < 1000; i++) {
            executor.submit(() -> {
                try {
                    counter.increment();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        System.out.println("  1000 concurrent increments — count: " + counter.get()
                + (counter.get() == 1000 ? " ✓" : " ← LOST UPDATES"));
    }

    // --- checkpoint 4: RequestContext with ThreadLocal ---

    private static void testRequestContext() throws InterruptedException, ExecutionException, TimeoutException {
        System.out.println("\n=== RequestContext (ThreadLocal) ===");

        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(3);

        // 3 threads each set their own userId — they must NOT see each other's value
        for (long userId = 1; userId <= 3; userId++) {
            final long id = userId;
            new Thread(() -> {
                try {
                    RequestContext.set(id);
                    Thread.sleep(50); // overlap with other threads
                    Long read = RequestContext.get();
                    synchronized (results) {
                        results.add("Thread-" + id + " read userId=" + read
                                + (read == id ? " ✓" : " ← WRONG (got " + read + ")"));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    RequestContext.clear(); // MUST clear — prevents memory leak in thread pools
                    latch.countDown();
                }
            }, "RequestThread-" + userId).start();
        }

        latch.await();
        results.forEach(r -> System.out.println("  " + r));

        // demonstrate memory leak: forget clear() in a thread pool
        System.out.println("\n  -- ThreadLocal memory leak demo --");
        ExecutorService pool = Executors.newFixedThreadPool(1); // single thread reused

        pool.submit(() -> {
            RequestContext.set(42L);
            // forgot to call RequestContext.clear()
        }).get(1, TimeUnit.SECONDS);

        pool.submit(() -> {
            Long leaked = RequestContext.get();
            System.out.println("  Reused thread sees stale userId=" + leaked
                    + " (should be null — this is the memory leak!)");
            RequestContext.clear(); // clean up now
        }).get(1, TimeUnit.SECONDS);

        pool.shutdown();
    }

    // --- checkpoint 5: OrderQueue producer-consumer ---

    private static void testOrderQueue() throws InterruptedException {
        System.out.println("\n=== OrderQueue (LinkedBlockingQueue + Poison Pill) ===");
        System.out.println("  5 producers x 4 orders = 20 total | 3 consumers\n");
        new OrderQueue().start();
    }
}
