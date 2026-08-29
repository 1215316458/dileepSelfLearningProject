package com.ecommerce.domain.concurrency;

import com.ecommerce.domain.enums.Category;
import com.ecommerce.domain.enums.Role;
import com.ecommerce.domain.model.CartItem;
import com.ecommerce.domain.model.Order;
import com.ecommerce.domain.model.Product;
import com.ecommerce.domain.model.User;
import com.ecommerce.domain.repository.OrderRepository;
import com.ecommerce.domain.repository.ProductRepository;
import com.ecommerce.domain.repository.UserRepository;
import com.ecommerce.domain.repository.concurrency.InventoryManager;
import com.ecommerce.domain.streams.OrderAnalytics;
import com.ecommerce.domain.streams.ProductAnalytics;
import com.ecommerce.domain.streams.SalesReportCollector;
import com.ecommerce.domain.utility.DataPersistence;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class EcommerceSimulator {

    private final ProductRepository productRepo   = new ProductRepository();
    private final UserRepository    userRepo      = new UserRepository();
    private final OrderRepository   orderRepo     = new OrderRepository(userRepo);
    private final InventoryManager  inventory     = new InventoryManager();
    private final EcommerceScheduler scheduler;
    private final OrderProcessingService orderService;

    private final AtomicInteger processedOrders = new AtomicInteger(0);
    private final AtomicInteger failedOrders    = new AtomicInteger(0);

    public EcommerceSimulator() {
        this.scheduler    = new EcommerceScheduler(productRepo, inventory);
        this.orderService = new OrderProcessingService(inventory);
    }

    public void run() throws Exception {
        System.out.println("========================================");
        System.out.println("   ECOMMERCE SIMULATOR — Day 12 Checkpoint");
        System.out.println("========================================\n");

        // 1. Seed data
        System.out.println("--- Step 1: Seeding 50 products and 20 users ---");
        List<Product> products = seedProducts();
        List<User>    users    = seedUsers();
        System.out.println("  Products: " + productRepo.count());
        System.out.println("  Users:    " + userRepo.count());

        // 2. Start scheduled tasks
        System.out.println("\n--- Step 2: Starting scheduled tasks ---");
        scheduler.start();
        users.forEach(u -> scheduler.addSession(u.getIdentity()));
        System.out.println("  Scheduler started with " + users.size() + " active sessions");

        // 3. Flash sale: 200 threads buying 100 limited items
        System.out.println("\n--- Step 3: Flash sale (200 buyers, 100 items) ---");
        FlashSaleManager flashSale = new FlashSaleManager(100, 100, 10);
        ExecutorService salePool = Executors.newFixedThreadPool(50);
        for (int i = 1; i <= 3; i++) flashSale.serviceReady(); // init services
        for (int i = 1; i <= 200; i++) {
            final long buyerId = i;
            salePool.submit(() -> flashSale.attemptPurchase(buyerId));
        }
        salePool.shutdown();
        salePool.awaitTermination(15, TimeUnit.SECONDS);
        System.out.println("  Sold: " + flashSale.getSuccessCount() + " | Rejected: " + flashSale.getFailCount());

        // 4. 50 concurrent orders through async pipeline
        System.out.println("\n--- Step 4: 50 concurrent orders via CompletableFuture ---");
        Product laptop = products.get(0);
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            Order order = new Order.Builder()
                    .id((long) i).userId((long) (i % 20 + 1))
                    .addItem(new CartItem(laptop, 1))
                    .shippingAddress("Address-" + i).build();
            orderRepo.save(order);
            futures.add(orderService.processOrder(order));
        }
        long start = System.currentTimeMillis();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
        futures.forEach(f -> {
            try {
                String r = f.get();
                if (r.startsWith("SUCCESS")) processedOrders.incrementAndGet();
                else failedOrders.incrementAndGet();
            } catch (Exception e) { failedOrders.incrementAndGet(); }
        });
        System.out.println("  Processed in " + (System.currentTimeMillis() - start) + "ms");

        // 5. Stream analytics
        System.out.println("\n--- Step 5: Stream analytics ---");
        List<Order> allOrders = orderRepo.findAll();
        System.out.println("  Category distribution: " + ProductAnalytics.categoryDistribution(products));
        System.out.println("  Sales report: " + allOrders.stream().collect(new SalesReportCollector()));

        // 6. Serialize and deserialize state
        System.out.println("\n--- Step 6: Serialize → Deserialize ---");
        DataPersistence.serialize(products.get(0), "simulator_product.ser");
        Product loaded = DataPersistence.deserialize("simulator_product.ser");
        System.out.println("  Serialized: " + products.get(0).getName());
        System.out.println("  Deserialized: " + loaded.getName() + " ✓");

        // 7. ForkJoin inventory value
        System.out.println("\n--- Step 7: ForkJoin inventory value ---");
        BigDecimal forkJoinValue = scheduler.calculateInventoryValue(products);
        BigDecimal streamValue   = products.stream()
                .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("  ForkJoin result: $" + forkJoinValue);
        System.out.println("  Stream result:   $" + streamValue);
        System.out.println("  Match: " + (forkJoinValue.compareTo(streamValue) == 0));

        // 8. Final summary
        System.out.println("\n--- Final Summary ---");
        System.out.println("  Orders processed: " + processedOrders.get());
        System.out.println("  Orders failed:    " + failedOrders.get());
        System.out.println("  Scheduler tasks run: " + scheduler.getTaskRunCount());

        // cleanup
        scheduler.stop();
        orderService.shutdown();
    }

    private List<Product> seedProducts() {
        Category[] cats = Category.values();
        List<Product> products = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            Category cat   = cats[i % cats.length];
            BigDecimal price = new BigDecimal(20 + (i * 15) + ".00");
            int stock      = (i % 7 == 0) ? 3 : 20 + i; // some low-stock items
            Product p = new Product((long) i, "Product-" + i, "Description", price, stock, cat);
            productRepo.save(p);
            inventory.addProduct(p);
            products.add(p);
        }
        return products;
    }

    private List<User> seedUsers() {
        Role[] roles = { Role.CUSTOMER, Role.CUSTOMER, Role.CUSTOMER, Role.SELLER, Role.ADMIN };
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            User u = new User((long) i, "user" + i, "user" + i + "@example.com", "pass", roles[i % roles.length]);
            userRepo.save(u);
            users.add(u);
        }
        return users;
    }
}
