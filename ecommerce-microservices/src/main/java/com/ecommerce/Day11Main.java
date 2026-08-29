package com.ecommerce;

import com.ecommerce.domain.concurrency.DeadlockDemo;
import com.ecommerce.domain.concurrency.FlashSaleManager;
import com.ecommerce.domain.concurrency.OrderProcessingService;
import com.ecommerce.domain.enums.Category;
import com.ecommerce.domain.enums.OrderStatus;
import com.ecommerce.domain.model.CartItem;
import com.ecommerce.domain.model.Order;
import com.ecommerce.domain.model.Product;
import com.ecommerce.domain.repository.concurrency.InventoryManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Day11Main {

    public static void main(String[] args) throws Exception {
        testCompletableFuturePipeline();
        testFlashSale();
        testDeadlock();
    }

    // --- checkpoint 1: CompletableFuture async pipeline ---

    private static void testCompletableFuturePipeline() throws Exception {
        System.out.println("=== CompletableFuture Pipeline ===");

        InventoryManager inventory = new InventoryManager();
        Product product = new Product(1L, "Laptop", "Desc", new BigDecimal("999.00"), 200, Category.ELECTRONICS);
        inventory.addProduct(product);

        OrderProcessingService service = new OrderProcessingService(inventory);

        // single order pipeline
        System.out.println("\n  -- Single order pipeline --");
        Order order = buildOrder(1L, 1L, product);
        String result = service.processOrder(order).get(5, TimeUnit.SECONDS);
        System.out.println("  " + result);

        // order that triggers payment failure (id % 10 == 0)
        System.out.println("\n  -- Payment failure + compensation --");
        Order failOrder = buildOrder(10L, 2L, product);
        String failResult = service.processOrder(failOrder).get(5, TimeUnit.SECONDS);
        System.out.println("  " + failResult);
        System.out.println("  Stock after compensation (should be 199): " + inventory.getStock(1L));

        // allOf — batch 10 orders in parallel
        System.out.println("\n  -- allOf: 10 orders in parallel --");
        List<Order> batch = new ArrayList<>();
        for (int i = 2; i <= 11; i++) {
            batch.add(buildOrder((long) i, (long) i, product));
        }
        long start = System.currentTimeMillis();
        service.processBatch(batch).get(10, TimeUnit.SECONDS);
        System.out.println("  10 orders processed in " + (System.currentTimeMillis() - start) + "ms");

        // anyOf — race two payment providers
        System.out.println("\n  -- anyOf: race two payment providers --");
        Object winner = service.racePaymentProviders(order).get(5, TimeUnit.SECONDS);
        System.out.println("  Winner: " + winner);

        service.shutdown();
    }

    // --- checkpoint 2: FlashSaleManager ---

    private static void testFlashSale() throws InterruptedException {
        System.out.println("\n=== FlashSaleManager ===");
        System.out.println("  100 items | 200 buyers | max 100 concurrent | batch size 10\n");

        // 100 items, max 100 concurrent buyers, batch every 10
        FlashSaleManager saleManager = new FlashSaleManager(100, 100, 10);

        // simulate 3 services initialising (CountDownLatch)
        ExecutorService initPool = Executors.newFixedThreadPool(3);
        for (int i = 1; i <= 3; i++) {
            final int serviceId = i;
            initPool.submit(() -> {
                try { Thread.sleep(10 * serviceId); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                System.out.println("  Service-" + serviceId + " ready");
                saleManager.serviceReady();
            });
        }
        initPool.shutdown();

        // 200 buyer threads — only 100 items available, so 100 succeed and 100 fail
        ExecutorService buyerPool = Executors.newFixedThreadPool(50);
        for (int i = 1; i <= 200; i++) {
            final long buyerId = i;
            buyerPool.submit(() -> saleManager.attemptPurchase(buyerId));
        }

        buyerPool.shutdown();
        buyerPool.awaitTermination(15, TimeUnit.SECONDS);

        System.out.println("\n  Results:");
        System.out.println("  Successful purchases: " + saleManager.getSuccessCount() + " (expect 100)");
        System.out.println("  Failed (sold out):    " + saleManager.getFailCount()    + " (expect 100)");
        System.out.println("  Remaining stock:      " + saleManager.getRemainingStock() + " (expect 0)");
        System.out.println("  Total = 200: " + (saleManager.getSuccessCount() + saleManager.getFailCount() == 200));
    }

    // --- checkpoint 3: Deadlock demo ---

    private static void testDeadlock() throws InterruptedException {
        System.out.println("\n=== Deadlock Demo ===");
        DeadlockDemo demo = new DeadlockDemo();

        System.out.println("\n  -- Deadlock scenario (will detect after 300ms) --");
        demo.deadlockScenario();

        System.out.println("\n  -- Fixed: consistent lock ordering --");
        demo.fixedScenario();

        System.out.println("\n  -- Fixed: tryLock with timeout --");
        demo.tryLockScenario();
    }

    // --- helpers ---

    private static Order buildOrder(Long orderId, Long userId, Product product) {
        return new Order.Builder()
                .id(orderId)
                .userId(userId)
                .addItem(new CartItem(product, 1))
                .status(OrderStatus.PENDING)
                .shippingAddress("Address-" + userId)
                .build();
    }
}
