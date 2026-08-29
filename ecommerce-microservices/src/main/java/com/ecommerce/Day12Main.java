package com.ecommerce;

import com.ecommerce.domain.concurrency.EcommerceScheduler;
import com.ecommerce.domain.concurrency.EcommerceSimulator;
import com.ecommerce.domain.enums.Category;
import com.ecommerce.domain.model.Product;
import com.ecommerce.domain.repository.ProductRepository;
import com.ecommerce.domain.repository.concurrency.InventoryManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Day12Main {

    public static void main(String[] args) throws Exception {
        testScheduler();
        testForkJoin();
        runSimulator();
    }

    // --- checkpoint 1: ScheduledExecutorService ---

    private static void testScheduler() throws InterruptedException {
        System.out.println("=== ScheduledExecutorService ===\n");

        ProductRepository repo      = new ProductRepository();
        InventoryManager  inventory = new InventoryManager();

        // seed some low-stock products to trigger the alert
        for (int i = 1; i <= 10; i++) {
            int stock = (i <= 3) ? 2 : 30; // first 3 are low stock
            Product p = new Product((long) i, "Product-" + i, "Desc",
                    new BigDecimal("50.00"), stock, Category.ELECTRONICS);
            repo.save(p);
            inventory.addProduct(p);
        }

        EcommerceScheduler scheduler = new EcommerceScheduler(repo, inventory);
        scheduler.addSession(1L);
        scheduler.addSession(2L);
        scheduler.start();

        // let scheduler run for 6 seconds — enough for low-stock check (every 5s) to fire once
        Thread.sleep(6_000);
        scheduler.stop();

        System.out.println("  Scheduler tasks fired: " + scheduler.getTaskRunCount());
    }

    // --- checkpoint 2: ForkJoinPool ---

    private static void testForkJoin() {
        System.out.println("\n=== ForkJoinPool (RecursiveTask) ===\n");

        ProductRepository repo      = new ProductRepository();
        InventoryManager  inventory = new InventoryManager();
        EcommerceScheduler scheduler = new EcommerceScheduler(repo, inventory);

        List<Product> products = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            Product p = new Product((long) i, "Product-" + i, "Desc",
                    new BigDecimal(10 + i + ".00"), i * 2, Category.ELECTRONICS);
            products.add(p);
        }

        BigDecimal forkJoinResult = scheduler.calculateInventoryValue(products);
        BigDecimal streamResult   = products.stream()
                .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        System.out.println("  ForkJoin result: $" + forkJoinResult);
        System.out.println("  Stream result:   $" + streamResult);
        System.out.println("  Match: " + (forkJoinResult.compareTo(streamResult) == 0 ? "✓" : "✗"));
    }

    // --- checkpoint 3: Full EcommerceSimulator ---

    private static void runSimulator() throws Exception {
        System.out.println("\n");
        new EcommerceSimulator().run();
    }
}
