package com.ecommerce;

import com.ecommerce.domain.cache.ProductCache;
import com.ecommerce.domain.enums.Category;
import com.ecommerce.domain.enums.Role;
import com.ecommerce.domain.model.Product;
import com.ecommerce.domain.model.User;
import com.ecommerce.domain.tracker.RecentlyViewedTracker;
import com.ecommerce.domain.utility.DataPersistence;

import java.math.BigDecimal;

public class Day6Main {

    public static void main(String[] args) throws InterruptedException {
        testRecentlyViewedTracker();
        testProductCache();
        testDataPersistence();
    }

    // --- checkpoint 1: RecentlyViewedTracker ---

    private static void testRecentlyViewedTracker() {
        System.out.println("=== RecentlyViewedTracker ===");

        RecentlyViewedTracker tracker = new RecentlyViewedTracker();

        // view 12 products — only last 10 should be kept (1 and 2 evicted)
        System.out.println("\n  -- Viewing products 1 to 12 --");
        for (long i = 1; i <= 12; i++) {
            tracker.track(i);
        }
        System.out.println("  Size after viewing 12 products (max=10): " + tracker.size()); // 10

        // iterate — most recent first (12, 11, 10 ... 3)
        System.out.println("\n  -- Iteration (most recent first) --");
        for (Long id : tracker) {
            System.out.print("  " + id);
        }
        System.out.println();

        // verify 1 and 2 were evicted
        System.out.println("\n  -- Eviction check --");
        boolean has1 = false, has2 = false;
        for (Long id : tracker) {
            if (id == 1L) has1 = true;
            if (id == 2L) has2 = true;
        }
        System.out.println("  Contains id=1 (should be false): " + has1);
        System.out.println("  Contains id=2 (should be false): " + has2);

        // view a product already in tracker — should move to most recent (head of iteration)
        System.out.println("\n  -- Re-view product 5 (should move to most recent) --");
        tracker.track(5L);
        System.out.println("  First in iteration (should be 5): " + tracker.iterator().next());
        System.out.println("  Size unchanged (should be 10): " + tracker.size());
    }

    // --- checkpoint 2: ProductCache (WeakHashMap) ---

    private static void testProductCache() throws InterruptedException {
        System.out.println("\n=== ProductCache (WeakHashMap) ===");

        ProductCache cache = new ProductCache();

        // use new Long() to ensure objects are on heap — NOT JVM cached range (-128 to 127)
        // JVM caches Long values -128 to 127, those always have strong refs and won't be GC'd
        Long id1 = new Long(1000);
        Long id2 = new Long(2000);
        Long id3 = new Long(3000); // id3 keeps its strong reference — should survive GC

        cache.put(id1, new Product(1000L, "Laptop", "Gaming Laptop", new BigDecimal("1000.00"), 10, Category.ELECTRONICS));
        cache.put(id2, new Product(2000L, "Phone",  "Smartphone",    new BigDecimal("800.00"),  20, Category.ELECTRONICS));
        cache.put(id3, new Product(3000L, "TV",     "4K Smart TV",   new BigDecimal("750.00"),  5,  Category.ELECTRONICS));

        System.out.println("  Before GC — cache size: " + cache.size()); // 3

        // remove strong references to id1 and id2 — GC can now collect them
        id1 = null;
        id2 = null;
        // id3 still has a strong reference — entry should survive

        System.gc();
        Thread.sleep(100); // give GC a moment to run

        System.out.println("  After GC  — cache size: " + cache.size()); // likely 1
        System.out.println("  id3 still in cache (should be true): " + (cache.get(id3) != null));
        System.out.println("  Note: WeakHashMap auto-removed entries whose keys had no strong references");
    }

    // --- checkpoint 3: DataPersistence (Serialization) ---

    private static void testDataPersistence() {
        System.out.println("\n=== DataPersistence (Serialization) ===");

        User user = new User(1L, "alice", "alice@example.com", "secret123", Role.CUSTOMER);
        Product product = new Product(1L, "Laptop", "Gaming Laptop", new BigDecimal("1000.00"), 10, Category.ELECTRONICS);

        // --- serialize ---
        System.out.println("\n  -- Serialize User --");
        System.out.println("  Before: " + user);
        System.out.println("  Password before: " + user.getPassword()); // secret123
        DataPersistence.serialize(user, "user.ser");

        // --- deserialize ---
        System.out.println("\n  -- Deserialize User --");
        User loadedUser = DataPersistence.deserialize("user.ser");
        System.out.println("  After:  " + loadedUser);
        System.out.println("  Password after (should be null — transient): " + loadedUser.getPassword()); // null
        System.out.println("  Email intact: " + loadedUser.getEmail());       // alice@example.com
        System.out.println("  Role intact:  " + loadedUser.getRole());        // CUSTOMER
        System.out.println("  Id intact:    " + loadedUser.getIdentity());    // 1

        // --- serialize and deserialize Product ---
        System.out.println("\n  -- Serialize/Deserialize Product --");
        DataPersistence.serialize(product, "product.ser");
        Product loadedProduct = DataPersistence.deserialize("product.ser");
        System.out.println("  Loaded: " + loadedProduct);
        System.out.println("  Price intact: $" + loadedProduct.getPrice()); // 1000.00
    }
}
