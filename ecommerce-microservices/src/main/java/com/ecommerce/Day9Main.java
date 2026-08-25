package com.ecommerce;

import com.ecommerce.domain.enums.Category;
import com.ecommerce.domain.model.CartItem;
import com.ecommerce.domain.model.Order;
import com.ecommerce.domain.model.Product;
import com.ecommerce.domain.streams.BulkPriceUpdater;
import com.ecommerce.domain.streams.FunctionalComposition;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Day9Main {

    public static void main(String[] args) throws InterruptedException {
        testBulkPriceUpdaterBenchmark();
        testThreadSafety();
        testFunctionalComposition();
    }

    // --- checkpoint 1: BulkPriceUpdater benchmark ---

    private static void testBulkPriceUpdaterBenchmark() {
        System.out.println("=== BulkPriceUpdater Benchmark ===");

        // 10,000 products — parallel should win due to CPU-bound work across many elements
        List<Product> largeList = generateProducts(10_000);
        long seqTime  = BulkPriceUpdater.updateSequential(largeList, BulkPriceUpdater.tenPercentDiscount);
        BulkPriceUpdater.updateSequential(largeList, BulkPriceUpdater.restorePrice); // restore
        long parTime  = BulkPriceUpdater.updateParallel(largeList, BulkPriceUpdater.tenPercentDiscount);

        System.out.println("\n  10,000 products:");
        System.out.println("  Sequential: " + seqTime + "ms");
        System.out.println("  Parallel:   " + parTime + "ms");
        System.out.println("  Parallel faster: " + (parTime < seqTime));

        // 100 products — sequential should win: parallel overhead (thread splitting, merging)
        // outweighs the benefit for small collections
        List<Product> smallList = generateProducts(100);
        long seqSmall = BulkPriceUpdater.updateSequential(smallList, BulkPriceUpdater.tenPercentDiscount);
        BulkPriceUpdater.updateSequential(smallList, BulkPriceUpdater.restorePrice);
        long parSmall = BulkPriceUpdater.updateParallel(smallList, BulkPriceUpdater.tenPercentDiscount);

        System.out.println("\n  100 products:");
        System.out.println("  Sequential: " + seqSmall + "ms");
        System.out.println("  Parallel:   " + parSmall + "ms");
        System.out.println("  Sequential faster (or equal): " + (seqSmall <= parSmall));
    }

    // --- checkpoint 2: Thread-safety demo ---

    private static void testThreadSafety() throws InterruptedException {
        System.out.println("\n=== Thread Safety Demo ===");

        // BROKEN: ArrayList is not thread-safe — parallel stream writes from multiple threads
        // causes race conditions: lost updates, ArrayIndexOutOfBoundsException, or wrong size
        List<Integer> unsafeList = new ArrayList<>();
        IntStream.range(0, 10_000).parallel().forEach(unsafeList::add);
        System.out.println("\n  ArrayList (unsafe) — expected 10000, got: " + unsafeList.size()
                + (unsafeList.size() != 10_000 ? " ← DATA CORRUPTION" : " (lucky run, try again)"));

        // SAFE: ConcurrentLinkedQueue uses lock-free CAS operations — thread-safe for concurrent adds
        ConcurrentLinkedQueue<Integer> safeQueue = new ConcurrentLinkedQueue<>();
        IntStream.range(0, 10_000).parallel().forEach(safeQueue::add);
        System.out.println("  ConcurrentLinkedQueue (safe) — expected 10000, got: " + safeQueue.size()
                + (safeQueue.size() == 10_000 ? " ✓" : " ← UNEXPECTED"));

        // SAFE alternative: collect() — each thread builds its own partial list, then merges
        // This is the idiomatic way — avoid shared mutable state entirely
        List<Integer> safeCollect = IntStream.range(0, 10_000).parallel()
                .boxed()
                .collect(Collectors.toList());
        System.out.println("  Collectors.toList() (safe)   — expected 10000, got: " + safeCollect.size() + " ✓");
    }

    // --- checkpoint 3: Functional composition ---

    private static void testFunctionalComposition() {
        System.out.println("\n=== Functional Composition ===");

        // Function.andThen — discount first, then round
        BigDecimal price = new BigDecimal("99.99");
        BigDecimal afterAndThen = FunctionalComposition.applyDiscountThenRound.apply(price);
        System.out.println("\n  Function.andThen (discount then round):");
        System.out.println("  $" + price + " -> $" + afterAndThen);

        // Function.compose — round first, then discount
        BigDecimal afterCompose = FunctionalComposition.roundThenApplyDiscount.apply(price);
        System.out.println("  Function.compose (round then discount):");
        System.out.println("  $" + price + " -> $" + afterCompose);

        // Predicate.and — affordable electronics in stock
        List<Product> products = generateProducts(20);
        long affordableElectronics = products.stream()
                .filter(FunctionalComposition.affordableElectronicsInStock)
                .count();
        System.out.println("\n  Predicate.and (affordable electronics in stock): " + affordableElectronics);

        // Predicate.or — books or sports
        long booksOrSports = products.stream()
                .filter(FunctionalComposition.booksOrSports)
                .count();
        System.out.println("  Predicate.or (books or sports): " + booksOrSports);

        // Predicate.negate — out of stock
        long outOfStock = products.stream()
                .filter(Predicate.not(p -> p.getStock() > 0))
                .count();
        System.out.println("  Predicate.negate (out of stock): " + outOfStock);

        // Supplier<Order> — deferred creation, called twice to show each call creates a new Order
        Product sampleProduct = products.get(0);
        var supplier = FunctionalComposition.testOrderSupplier(sampleProduct);
        Order order1 = supplier.get();
        Order order2 = supplier.get();
        System.out.println("\n  Supplier<Order> — two calls produce two distinct orders:");
        System.out.println("  order1 id=" + order1.getIdentity() + " userId=" + order1.getUserId());
        System.out.println("  order2 id=" + order2.getIdentity() + " userId=" + order2.getUserId());
        System.out.println("  Same instance: " + (order1 == order2)); // false — Supplier creates new each time
    }

    // --- helpers ---

    // Generates N products spread across all categories with varying prices and stock
    private static List<Product> generateProducts(int count) {
        Category[] categories = Category.values();
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Category cat = categories[i % categories.length];
            BigDecimal price = new BigDecimal(50 + (i % 20) * 25 + ".00"); // $50 to $525
            int stock = (i % 5 == 0) ? 0 : (i % 50) + 1;                  // every 5th is out of stock
            products.add(new Product((long) i + 1, "Product-" + (i + 1), "Desc", price, stock, cat));
        }
        return products;
    }
}
