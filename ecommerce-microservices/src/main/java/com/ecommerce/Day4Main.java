package com.ecommerce;

import com.ecommerce.domain.enums.Category;
import com.ecommerce.domain.model.Product;
import com.ecommerce.domain.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Day4Main {

    public static void main(String[] args) {
        ProductRepository repo = new ProductRepository();
        seedProducts(repo);

        testFindById(repo);
        testFindByCategory(repo);
        testFindByPriceRange(repo);
        testFindByNameContaining(repo);
        testNaturalOrder(repo);
        testComparators(repo);
        testDeleteAndCount(repo);
    }

    // --- seed ---

    private static void seedProducts(ProductRepository repo) {
        repo.save(new Product(1L,  "iPhone 15",       "Apple smartphone",    new BigDecimal("999.00"),  50,  Category.ELECTRONICS));
        repo.save(new Product(2L,  "Samsung TV",       "4K Smart TV",         new BigDecimal("750.00"),  20,  Category.ELECTRONICS));
        repo.save(new Product(3L,  "Effective Java",   "Joshua Bloch",        new BigDecimal("45.00"),   200, Category.BOOKS));
        repo.save(new Product(4L,  "Clean Code",       "Robert C. Martin",    new BigDecimal("40.00"),   150, Category.BOOKS));
        repo.save(new Product(5L,  "Nike Shoes",       "Running shoes",       new BigDecimal("120.00"),  80,  Category.SPORTS));
        repo.save(new Product(6L,  "Yoga Mat",         "Non-slip mat",        new BigDecimal("35.00"),   60,  Category.SPORTS));
        repo.save(new Product(7L,  "Levi's Jeans",     "Slim fit jeans",      new BigDecimal("89.00"),   100, Category.CLOTHING));
        repo.save(new Product(8L,  "Winter Jacket",    "Waterproof jacket",   new BigDecimal("199.00"),  30,  Category.CLOTHING));
        repo.save(new Product(9L,  "MacBook Pro",      "Apple laptop",        new BigDecimal("1999.00"), 15,  Category.ELECTRONICS));
        repo.save(new Product(10L, "Design Patterns",  "Gang of Four",        new BigDecimal("55.00"),   120, Category.BOOKS));

        System.out.println("=== Seeded " + repo.count() + " products ===\n");
    }

    // --- tests ---

    private static void testFindById(ProductRepository repo) {
        System.out.println("=== findById ===");

        // found
        repo.findById(1L).ifPresent(p -> System.out.println("  Found:     " + p));

        // not found — Optional forces us to handle the empty case, no NullPointerException
        System.out.println("  Not found: " + repo.findById(99L).orElse(null));
        System.out.println();
    }

    private static void testFindByCategory(ProductRepository repo) {
        System.out.println("=== findByCategory ===");

        List<Product> electronics = repo.findByCategory(Category.ELECTRONICS);
        System.out.println("  ELECTRONICS (" + electronics.size() + "):");
        electronics.forEach(p -> System.out.println("    " + p));

        List<Product> books = repo.findByCategory(Category.BOOKS);
        System.out.println("  BOOKS (" + books.size() + "):");
        books.forEach(p -> System.out.println("    " + p));
        System.out.println();
    }

    private static void testFindByPriceRange(ProductRepository repo) {
        System.out.println("=== findByPriceRange ($50 - $200) ===");

        // TreeMap.subMap() — only scans the relevant price slice, not all products
        List<Product> inRange = repo.findByPriceRange(new BigDecimal("50.00"), new BigDecimal("200.00"));
        inRange.forEach(p -> System.out.println("  " + p));
        System.out.println();
    }

    private static void testFindByNameContaining(ProductRepository repo) {
        System.out.println("=== findByNameContaining ===");

        System.out.println("  keyword='java':");
        repo.findByNameContaining("java").forEach(p -> System.out.println("    " + p));

        System.out.println("  keyword='pro':");
        repo.findByNameContaining("pro").forEach(p -> System.out.println("    " + p));
        System.out.println();
    }

    private static void testNaturalOrder(ProductRepository repo) {
        System.out.println("=== Natural Order (Comparable — alphabetical by name) ===");

        // Product implements Comparable<Product> — Collections.sort() uses compareTo()
        List<Product> sorted = new ArrayList<>(repo.findAll());
        Collections.sort(sorted);
        sorted.forEach(p -> System.out.println("  " + p.getName()));
        System.out.println();
    }

    private static void testComparators(ProductRepository repo) {
        System.out.println("=== Static Comparators ===");

        List<Product> all = new ArrayList<>(repo.findAll());

        // BY_PRICE — cheapest first
        all.sort(Product.BY_PRICE);
        System.out.println("  BY_PRICE (cheapest first):");
        all.forEach(p -> System.out.println("    $" + p.getPrice() + " — " + p.getName()));

        // BY_STOCK — lowest stock first (useful for reorder alerts)
        all.sort(Product.BY_STOCK);
        System.out.println("\n  BY_STOCK (lowest first):");
        all.forEach(p -> System.out.println("    stock=" + p.getStock() + " — " + p.getName()));

        // BY_CREATED_DATE — oldest first
        all.sort(Product.BY_CREATED_DATE);
        System.out.println("\n  BY_CREATED_DATE (oldest first):");
        all.forEach(p -> System.out.println("    " + p.getCreatedAt().toLocalTime() + " — " + p.getName()));
        System.out.println();
    }

    private static void testDeleteAndCount(ProductRepository repo) {
        System.out.println("=== delete + count + existsById ===");

        System.out.println("  Before delete — count: " + repo.count());
        System.out.println("  existsById(1): " + repo.existsById(1L));

        repo.deleteById(1L);

        System.out.println("  After delete  — count: " + repo.count());
        System.out.println("  existsById(1): " + repo.existsById(1L));

        // findById after delete returns empty Optional — no crash
        System.out.println("  findById(1):   " + repo.findById(1L));

        // verify price index is also cleaned up — iPhone was $999, no other product at that price
        List<Product> atIphonePrice = repo.findByPriceRange(new BigDecimal("999.00"), new BigDecimal("999.00"));
        System.out.println("  Products at $999 after delete: " + atIphonePrice.size() + " (should be 0)");
    }
}
