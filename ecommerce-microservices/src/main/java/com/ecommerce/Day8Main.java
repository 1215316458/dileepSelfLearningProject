package com.ecommerce;

import com.ecommerce.domain.enums.Category;
import com.ecommerce.domain.enums.OrderStatus;
import com.ecommerce.domain.model.CartItem;
import com.ecommerce.domain.model.Order;
import com.ecommerce.domain.model.Product;
import com.ecommerce.domain.streams.InventoryAnalytics;
import com.ecommerce.domain.streams.OrderAnalytics;
import com.ecommerce.domain.streams.SalesReport;
import com.ecommerce.domain.streams.SalesReportCollector;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Day8Main {

    public static void main(String[] args) {
        List<Product> products = seedProducts();
        List<Order> orders = seedOrders(products);

        testOrderAnalytics(orders);
        testInventoryAnalytics(products, orders);
        testOptionalChaining(products, orders);
        testSalesReportCollector(orders);
    }

    // --- checkpoint 1: OrderAnalytics ---

    private static void testOrderAnalytics(List<Order> orders) {
        System.out.println("=== OrderAnalytics ===");

        System.out.println("\n  -- Revenue per category --");
        OrderAnalytics.revenuePerCategory(orders)
                .forEach((cat, rev) -> System.out.println("  " + cat + ": $" + rev));

        System.out.println("\n  -- Monthly revenue trend --");
        OrderAnalytics.revenuePerYearMonth(orders)
                .forEach((month, rev) -> System.out.println("  " + month + ": $" + rev));

        System.out.println("\n  -- Average order value per user --");
        OrderAnalytics.averageOrderValuePerUser(orders)
                .forEach((userId, avg) -> System.out.println("  userId=" + userId + " avg=$" + avg));

        System.out.println("\n  -- High-value customers (spend > $500) --");
        OrderAnalytics.highValueCustomers(orders, new BigDecimal("500"))
                .forEach(userId -> System.out.println("  userId=" + userId));

        System.out.println("\n  -- Order status distribution --");
        OrderAnalytics.orderStatusDistribution(orders)
                .forEach((delivered, statusMap) ->
                        System.out.println("  delivered=" + delivered + " -> " + statusMap));

        System.out.println("\n  -- Revenue per category per month --");
        OrderAnalytics.revenuePerCategoryPerMonth(orders)
                .forEach((cat, monthMap) -> monthMap
                        .forEach((month, rev) -> System.out.println("  " + cat + " / " + month + ": $" + rev)));
    }

    // --- checkpoint 2: InventoryAnalytics ---

    private static void testInventoryAnalytics(List<Product> products, List<Order> orders) {
        System.out.println("\n=== InventoryAnalytics ===");

        System.out.println("\n  -- Low stock alerts (threshold=15) --");
        InventoryAnalytics.lowStockAlerts(products, 15)
                .forEach(p -> System.out.println("  " + p.getName() + " stock=" + p.getStock()));

        System.out.println("\n  -- Total inventory value --");
        System.out.println("  $" + InventoryAnalytics.totalInventoryValue(products));

        System.out.println("\n  -- Dead stock (no orders in last 30 days) --");
        // all orders were just created (now), so shift cutoff to future to simulate dead stock
        LocalDateTime future = LocalDateTime.now().plusDays(1);
        InventoryAnalytics.deadStock(products, orders, future)
                .forEach(p -> System.out.println("  " + p.getName()));
    }

    // --- checkpoint 3: Optional chaining ---

    private static void testOptionalChaining(List<Product> products, List<Order> orders) {
        System.out.println("\n=== Optional Chaining ===");

        // findCheapestInCategory — present case
        InventoryAnalytics.findCheapestInCategory(products, Category.ELECTRONICS)
                .ifPresent(p -> System.out.println("\n  Cheapest ELECTRONICS: " + p.getName() + " $" + p.getPrice()));

        // findCheapestInCategory — absent case (no FOOD products with stock)
        String result = InventoryAnalytics.findCheapestInCategory(products, Category.FOOD)
                .map(p -> p.getName() + " $" + p.getPrice())
                .orElse("No products found");
        System.out.println("  Cheapest FOOD: " + result);

        // getOrderWithHighestValue — present case
        OrderAnalytics.getOrderWithHighestValue(1L, orders)
                .ifPresent(o -> System.out.println("  Highest order for userId=1: $" + o.getTotalAmount()));

        // getOrderWithHighestValue — absent case
        String noOrder = OrderAnalytics.getOrderWithHighestValue(99L, orders)
                .map(o -> "$" + o.getTotalAmount())
                .orElse("No orders found for userId=99");
        System.out.println("  " + noOrder);
    }

    // --- checkpoint 4: SalesReportCollector ---

    private static void testSalesReportCollector(List<Order> orders) {
        System.out.println("\n=== SalesReportCollector ===");

        SalesReport report = orders.stream().collect(new SalesReportCollector());
        System.out.println("\n  " + report);

        // verify against manual calculation
        BigDecimal manualTotal = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("\n  Manual total:    $" + manualTotal);
        System.out.println("  Collector total: $" + report.getTotalRevenue());
        System.out.println("  Match: " + (manualTotal.compareTo(report.getTotalRevenue()) == 0));
    }

    // --- seed data ---

    private static List<Product> seedProducts() {
        return List.of(
            new Product(1L,  "Laptop Pro",      "High-end laptop",  new BigDecimal("1200.00"), 5,   Category.ELECTRONICS),
            new Product(2L,  "Laptop Basic",    "Budget laptop",    new BigDecimal("450.00"),  10,  Category.ELECTRONICS),
            new Product(3L,  "Smartphone Pro",  "Flagship phone",   new BigDecimal("999.00"),  8,   Category.ELECTRONICS),
            new Product(4L,  "Smartphone Lite", "Budget phone",     new BigDecimal("299.00"),  15,  Category.ELECTRONICS),
            new Product(5L,  "Headphones",      "Noise cancelling", new BigDecimal("199.00"),  0,   Category.ELECTRONICS),
            new Product(6L,  "T-Shirt",         "Cotton tee",       new BigDecimal("25.00"),   50,  Category.CLOTHING),
            new Product(7L,  "Jeans",           "Slim fit",         new BigDecimal("60.00"),   30,  Category.CLOTHING),
            new Product(8L,  "Jacket Pro",      "Winter jacket",    new BigDecimal("150.00"),  12,  Category.CLOTHING),
            new Product(9L,  "Sneakers",        "Running shoes",    new BigDecimal("90.00"),   0,   Category.CLOTHING),
            new Product(10L, "Dress",           "Summer dress",     new BigDecimal("45.00"),   20,  Category.CLOTHING),
            new Product(11L, "Java Book",       "Effective Java",   new BigDecimal("40.00"),   25,  Category.BOOKS),
            new Product(12L, "Design Patterns", "GoF patterns",     new BigDecimal("35.00"),   18,  Category.BOOKS),
            new Product(13L, "Clean Code",      "Robert Martin",    new BigDecimal("38.00"),   22,  Category.BOOKS),
            new Product(14L, "Spring Boot",     "Spring in Action", new BigDecimal("42.00"),   0,   Category.BOOKS),
            new Product(15L, "Kafka Guide",     "Kafka definitive", new BigDecimal("48.00"),   10,  Category.BOOKS),
            new Product(16L, "Protein Powder",  "Whey protein",     new BigDecimal("55.00"),   40,  Category.SPORTS),
            new Product(17L, "Yoga Mat",        "Non-slip mat",     new BigDecimal("30.00"),   35,  Category.SPORTS),
            new Product(18L, "Dumbbells",       "5kg pair",         new BigDecimal("75.00"),   20,  Category.SPORTS),
            new Product(19L, "Rice 5kg",        "Basmati rice",     new BigDecimal("12.00"),   100, Category.FOOD),
            new Product(20L, "Olive Oil",       "Extra virgin",     new BigDecimal("18.00"),   60,  Category.FOOD)
        );
    }

    private static List<Order> seedOrders(List<Product> products) {
        Order o1 = new Order.Builder().id(1L).userId(1L)
                .addItem(new CartItem(products.get(0), 1))   // Laptop Pro x1
                .addItem(new CartItem(products.get(2), 2))   // Smartphone Pro x2
                .shippingAddress("123 Main St").build();

        Order o2 = new Order.Builder().id(2L).userId(2L)
                .addItem(new CartItem(products.get(1), 1))   // Laptop Basic x1
                .addItem(new CartItem(products.get(10), 3))  // Java Book x3
                .shippingAddress("456 Oak Ave").build();

        Order o3 = new Order.Builder().id(3L).userId(3L)
                .addItem(new CartItem(products.get(5), 4))   // T-Shirt x4
                .addItem(new CartItem(products.get(6), 2))   // Jeans x2
                .shippingAddress("789 Pine Rd").build();

        Order o4 = new Order.Builder().id(4L).userId(1L)
                .addItem(new CartItem(products.get(2), 1))   // Smartphone Pro x1
                .addItem(new CartItem(products.get(11), 2))  // Design Patterns x2
                .shippingAddress("123 Main St").build();

        Order o5 = new Order.Builder().id(5L).userId(4L)
                .addItem(new CartItem(products.get(15), 2))  // Protein Powder x2
                .addItem(new CartItem(products.get(16), 1))  // Yoga Mat x1
                .shippingAddress("321 Elm St").build();

        Order o6 = new Order.Builder().id(6L).userId(5L)
                .addItem(new CartItem(products.get(18), 3))  // Rice x3
                .addItem(new CartItem(products.get(19), 2))  // Olive Oil x2
                .shippingAddress("654 Maple Dr").build();

        Order o7 = new Order.Builder().id(7L).userId(2L)
                .addItem(new CartItem(products.get(3), 1))   // Smartphone Lite x1
                .addItem(new CartItem(products.get(7), 1))   // Jacket Pro x1
                .shippingAddress("456 Oak Ave").build();

        Order o8 = new Order.Builder().id(8L).userId(6L)
                .addItem(new CartItem(products.get(12), 1))  // Clean Code x1
                .addItem(new CartItem(products.get(17), 2))  // Dumbbells x2
                .shippingAddress("987 Cedar Ln").build();

        Order o9 = new Order.Builder().id(9L).userId(7L)
                .addItem(new CartItem(products.get(0), 1))   // Laptop Pro x1
                .addItem(new CartItem(products.get(10), 1))  // Java Book x1
                .shippingAddress("111 Birch Blvd").build();

        Order o10 = new Order.Builder().id(10L).userId(8L)
                .status(OrderStatus.DELIVERED)
                .addItem(new CartItem(products.get(5), 3))   // T-Shirt x3
                .addItem(new CartItem(products.get(9), 2))   // Dress x2
                .shippingAddress("222 Walnut Way").build();

        return List.of(o1, o2, o3, o4, o5, o6, o7, o8, o9, o10);
    }
}
