package com.ecommerce;

import com.ecommerce.domain.dto.ProductDTO;
import com.ecommerce.domain.enums.Category;
import com.ecommerce.domain.enums.OrderStatus;
import com.ecommerce.domain.model.CartItem;
import com.ecommerce.domain.model.Order;
import com.ecommerce.domain.model.Product;
import com.ecommerce.domain.streams.ProductAnalytics;
import com.ecommerce.domain.streams.filter.ProductFilter;
import com.ecommerce.domain.streams.transformer.ProductTransformer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Day7Main {

    public static void main(String[] args) {
        List<Product> products = seedProducts();
        List<Order> orders = seedOrders(products);

        testProductFilter(products);
        testProductTransformer(products);
        testProductAnalytics(products, orders);
    }

    // --- checkpoint 1: ProductFilter ---

    private static void testProductFilter(List<Product> products) {
        System.out.println("=== ProductFilter ===");

        // single filter
        List<Product> electronics = products.stream()
                .filter(ProductFilter.byCategory(Category.ELECTRONICS))
                .collect(Collectors.toList());
        System.out.println("\n  Electronics count: " + electronics.size());

        // composed filter: ELECTRONICS + in stock + price range
        List<Product> composed = products.stream()
                .filter(ProductFilter.byCategory(Category.ELECTRONICS)
                        .and(ProductFilter.byInStock())
                        .and(ProductFilter.byPriceRange(new BigDecimal("100"), new BigDecimal("800"))))
                .collect(Collectors.toList());
        System.out.println("  Electronics, in stock, $100-$800: " + composed.size());

        // name search
        List<Product> named = products.stream()
                .filter(ProductFilter.byNameContains("pro"))
                .collect(Collectors.toList());
        System.out.println("  Name contains 'pro': " + named.size());

        // negate: out of stock
        List<Product> outOfStock = products.stream()
                .filter(ProductFilter.byInStock().negate())
                .collect(Collectors.toList());
        System.out.println("  Out of stock: " + outOfStock.size());
    }

    // --- checkpoint 2: ProductTransformer ---

    private static void testProductTransformer(List<Product> products) {
        System.out.println("\n=== ProductTransformer ===");

        ProductDTO summary = ProductTransformer.toSummary.apply(products.get(0));
        System.out.println("\n  toSummary    — id=" + summary.getId() + ", name=" + summary.getName() + ", price=$" + summary.getPrice() + ", description=" + summary.getDescription());

        ProductDTO detailed = ProductTransformer.toDetailed.apply(products.get(0));
        System.out.println("  toDetailed   — " + detailed);

        ProductDTO catalog = ProductTransformer.toCatalogEntry.apply(products.get(0));
        System.out.println("  toCatalogEntry — id=" + catalog.getId() + ", name=" + catalog.getName() + ", category=" + catalog.getCategory());

        // map entire list to summaries
        List<ProductDTO> summaries = products.stream()
                .map(ProductTransformer.toSummary)
                .collect(Collectors.toList());
        System.out.println("  Mapped " + summaries.size() + " products to summary DTOs");
    }

    // --- checkpoint 3: ProductAnalytics ---

    private static void testProductAnalytics(List<Product> products, List<Order> orders) {
        System.out.println("\n=== ProductAnalytics ===");

        // top 3 by quantity sold
        System.out.println("\n  -- Top 3 products by quantity sold --");
        ProductAnalytics.topNByQuantitySold(orders, 3)
                .forEach(e -> System.out.println("  productId=" + e.getKey() + " qty=" + e.getValue()));

        // never ordered
        List<Product> neverOrdered = ProductAnalytics.neverOrdered(products, orders);
        System.out.println("\n  -- Products never ordered: " + neverOrdered.size() + " --");
        neverOrdered.forEach(p -> System.out.println("  " + p.getName()));

        // category distribution
        System.out.println("\n  -- Category distribution --");
        ProductAnalytics.categoryDistribution(products)
                .forEach((cat, count) -> System.out.println("  " + cat + ": " + count));

        // price histogram
        System.out.println("\n  -- Price histogram --");
        ProductAnalytics.priceHistogram(products)
                .forEach((bucket, list) -> System.out.println("  " + bucket + ": " + list.size() + " products"));
    }

    // --- seed data ---

    private static List<Product> seedProducts() {
        return List.of(
            new Product(1L,  "Laptop Pro",      "High-end laptop",   new BigDecimal("1200.00"), 5,  Category.ELECTRONICS),
            new Product(2L,  "Laptop Basic",    "Budget laptop",     new BigDecimal("450.00"),  10, Category.ELECTRONICS),
            new Product(3L,  "Smartphone Pro",  "Flagship phone",    new BigDecimal("999.00"),  8,  Category.ELECTRONICS),
            new Product(4L,  "Smartphone Lite", "Budget phone",      new BigDecimal("299.00"),  15, Category.ELECTRONICS),
            new Product(5L,  "Headphones",      "Noise cancelling",  new BigDecimal("199.00"),  0,  Category.ELECTRONICS),
            new Product(6L,  "T-Shirt",         "Cotton tee",        new BigDecimal("25.00"),   50, Category.CLOTHING),
            new Product(7L,  "Jeans",           "Slim fit",          new BigDecimal("60.00"),   30, Category.CLOTHING),
            new Product(8L,  "Jacket Pro",      "Winter jacket",     new BigDecimal("150.00"),  12, Category.CLOTHING),
            new Product(9L,  "Sneakers",        "Running shoes",     new BigDecimal("90.00"),   0,  Category.CLOTHING),
            new Product(10L, "Dress",           "Summer dress",      new BigDecimal("45.00"),   20, Category.CLOTHING),
            new Product(11L, "Java Book",       "Effective Java",    new BigDecimal("40.00"),   25, Category.BOOKS),
            new Product(12L, "Design Patterns", "GoF patterns",      new BigDecimal("35.00"),   18, Category.BOOKS),
            new Product(13L, "Clean Code",      "Robert Martin",     new BigDecimal("38.00"),   22, Category.BOOKS),
            new Product(14L, "Spring Boot",     "Spring in Action",  new BigDecimal("42.00"),   0,  Category.BOOKS),
            new Product(15L, "Kafka Guide",     "Kafka definitive",  new BigDecimal("48.00"),   10, Category.BOOKS),
            new Product(16L, "Protein Powder",  "Whey protein",      new BigDecimal("55.00"),   40, Category.SPORTS),
            new Product(17L, "Yoga Mat",        "Non-slip mat",      new BigDecimal("30.00"),   35, Category.SPORTS),
            new Product(18L, "Dumbbells",       "5kg pair",          new BigDecimal("75.00"),   20, Category.SPORTS),
            new Product(19L, "Rice 5kg",        "Basmati rice",      new BigDecimal("12.00"),   100, Category.FOOD),
            new Product(20L, "Olive Oil",       "Extra virgin",      new BigDecimal("18.00"),   60, Category.FOOD)
        );
    }

    private static List<Order> seedOrders(List<Product> products) {
        // helper to get product by index
        Order o1 = new Order.Builder().id(1L).userId(1L)
                .addItem(new CartItem(products.get(0), 1))  // Laptop Pro x1
                .addItem(new CartItem(products.get(2), 2))  // Smartphone Pro x2
                .shippingAddress("123 Main St").build();

        Order o2 = new Order.Builder().id(2L).userId(2L)
                .addItem(new CartItem(products.get(1), 1))  // Laptop Basic x1
                .addItem(new CartItem(products.get(10), 3)) // Java Book x3
                .shippingAddress("456 Oak Ave").build();

        Order o3 = new Order.Builder().id(3L).userId(3L)
                .addItem(new CartItem(products.get(5), 4))  // T-Shirt x4
                .addItem(new CartItem(products.get(6), 2))  // Jeans x2
                .shippingAddress("789 Pine Rd").build();

        Order o4 = new Order.Builder().id(4L).userId(1L)
                .addItem(new CartItem(products.get(2), 1))  // Smartphone Pro x1
                .addItem(new CartItem(products.get(11), 2)) // Design Patterns x2
                .shippingAddress("123 Main St").build();

        Order o5 = new Order.Builder().id(5L).userId(4L)
                .addItem(new CartItem(products.get(15), 2)) // Protein Powder x2
                .addItem(new CartItem(products.get(16), 1)) // Yoga Mat x1
                .shippingAddress("321 Elm St").build();

        Order o6 = new Order.Builder().id(6L).userId(5L)
                .addItem(new CartItem(products.get(18), 3)) // Rice x3
                .addItem(new CartItem(products.get(19), 2)) // Olive Oil x2
                .shippingAddress("654 Maple Dr").build();

        Order o7 = new Order.Builder().id(7L).userId(2L)
                .addItem(new CartItem(products.get(3), 1))  // Smartphone Lite x1
                .addItem(new CartItem(products.get(7), 1))  // Jacket Pro x1
                .shippingAddress("456 Oak Ave").build();

        Order o8 = new Order.Builder().id(8L).userId(6L)
                .addItem(new CartItem(products.get(12), 1)) // Clean Code x1
                .addItem(new CartItem(products.get(17), 2)) // Dumbbells x2
                .shippingAddress("987 Cedar Ln").build();

        Order o9 = new Order.Builder().id(9L).userId(7L)
                .addItem(new CartItem(products.get(0), 1))  // Laptop Pro x1
                .addItem(new CartItem(products.get(10), 1)) // Java Book x1
                .shippingAddress("111 Birch Blvd").build();

        Order o10 = new Order.Builder().id(10L).userId(8L)
                .addItem(new CartItem(products.get(5), 3))  // T-Shirt x3
                .addItem(new CartItem(products.get(9), 2))  // Dress x2
                .shippingAddress("222 Walnut Way").build();

        return List.of(o1, o2, o3, o4, o5, o6, o7, o8, o9, o10);
    }
}
