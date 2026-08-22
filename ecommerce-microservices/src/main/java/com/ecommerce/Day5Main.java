package com.ecommerce;

import com.ecommerce.domain.enums.Category;
import com.ecommerce.domain.enums.OrderStatus;
import com.ecommerce.domain.enums.Role;
import com.ecommerce.domain.exception.DuplicateEmailException;
import com.ecommerce.domain.model.CartItem;
import com.ecommerce.domain.model.Order;
import com.ecommerce.domain.model.Product;
import com.ecommerce.domain.model.User;
import com.ecommerce.domain.pattern.strategy.BulkPricing;
import com.ecommerce.domain.pattern.strategy.PremiumUserPricing;
import com.ecommerce.domain.pattern.strategy.RegularPricing;
import com.ecommerce.domain.pattern.strategy.ShoppingCart;
import com.ecommerce.domain.repository.OrderRepository;
import com.ecommerce.domain.repository.UserRepository;

import java.math.BigDecimal;
import java.util.PriorityQueue;

public class Day5Main {

    public static void main(String[] args) {
        testUserRepository();
        testOrderRepository();
        testShoppingCart();
    }

    // --- checkpoint 1: UserRepository ---

    private static void testUserRepository() {
        System.out.println("=== UserRepository ===");

        UserRepository repo = new UserRepository();

        repo.save(new User(1L, "alice",   "alice@example.com",   "pass", Role.CUSTOMER));
        repo.save(new User(2L, "bob",     "bob@example.com",     "pass", Role.SELLER));
        repo.save(new User(3L, "charlie", "charlie@example.com", "pass", Role.ADMIN));
        repo.save(new User(4L, "diana",   "diana@example.com",   "pass", Role.CUSTOMER));

        // duplicate email → DuplicateEmailException [USER_001]
        System.out.println("\n  -- Duplicate email test --");
        try {
            repo.save(new User(5L, "eve", "alice@example.com", "pass", Role.CUSTOMER));
        } catch (DuplicateEmailException e) {
            System.out.println("  Caught: " + e); // [USER_001] Email already registered: alice@example.com
        }

        // findByEmail
        System.out.println("\n  -- findByEmail --");
        repo.findByEmail("bob@example.com")
            .ifPresent(u -> System.out.println("  Found: " + u));
        System.out.println("  Not found: " + repo.findByEmail("unknown@example.com"));

        // findByRole
        System.out.println("\n  -- findByRole(CUSTOMER) --");
        repo.findByRole(Role.CUSTOMER).forEach(u -> System.out.println("  " + u));

        System.out.println("\n  Total users: " + repo.count());
    }

    // --- checkpoint 2: OrderRepository ---

    private static void testOrderRepository() {
        System.out.println("\n=== OrderRepository ===");

        UserRepository userRepo = new UserRepository();
        userRepo.save(new User(1L, "alice",   "alice@example.com",   "pass", Role.CUSTOMER));
        userRepo.save(new User(2L, "bob",     "bob@example.com",     "pass", Role.CUSTOMER));
        userRepo.save(new User(3L, "charlie", "charlie@example.com", "pass", Role.ADMIN));
        userRepo.save(new User(4L, "diana",   "diana@example.com",   "pass", Role.SELLER));

        OrderRepository orderRepo = new OrderRepository(userRepo);

        Product laptop = new Product(1L, "Laptop", "Gaming Laptop", new BigDecimal("1000.00"), 10, Category.ELECTRONICS);
        Product book   = new Product(2L, "Clean Code", "R.C. Martin", new BigDecimal("40.00"), 50, Category.BOOKS);

        // alice places 2 orders, bob places 1
        orderRepo.save(new Order.Builder().id(1L).userId(1L).addItem(new CartItem(laptop, 1)).shippingAddress("123 Main St").build());
        orderRepo.save(new Order.Builder().id(2L).userId(2L).addItem(new CartItem(book, 2)).shippingAddress("456 Oak Ave").build());
        orderRepo.save(new Order.Builder().id(3L).userId(1L).addItem(new CartItem(book, 1)).shippingAddress("123 Main St").build());
        // charlie (ADMIN) and diana (SELLER) also have pending orders
        orderRepo.save(new Order.Builder().id(4L).userId(3L).addItem(new CartItem(laptop, 2)).shippingAddress("789 Pine Rd").build());
        orderRepo.save(new Order.Builder().id(5L).userId(4L).addItem(new CartItem(book, 5)).shippingAddress("321 Elm St").build());

        // findByUserId — chronological order (LinkedHashMap preserves insertion order)
        System.out.println("\n  -- findByUserId(alice=1) — chronological --");
        orderRepo.findByUserId(1L).forEach(o -> System.out.println("  " + o));

        // findByStatus
        System.out.println("\n  -- findByStatus(PENDING) --");
        orderRepo.findByStatus(OrderStatus.PENDING).forEach(o -> System.out.println("  " + o));

        // PriorityQueue — ADMIN first, then SELLER, then CUSTOMER
        System.out.println("\n  -- Processing queue (premium users first) --");
        PriorityQueue<Order> queue = orderRepo.buildProcessingQueue();
        while (!queue.isEmpty()) {
            Order o = queue.poll();
            String role = userRepo.findById(o.getUserId()).map(u -> u.getRole().name()).orElse("?");
            System.out.println("  Processing orderId=" + o.getIdentity() + " userId=" + o.getUserId() + " role=" + role);
        }
    }

    // --- checkpoint 3: ShoppingCart with PricingStrategy ---

    private static void testShoppingCart() {
        System.out.println("\n=== ShoppingCart ===");

        Product phone  = new Product(1L, "Phone",  "Smartphone",    new BigDecimal("800.00"), 30, Category.ELECTRONICS);
        Product laptop = new Product(2L, "Laptop", "Gaming Laptop", new BigDecimal("1000.00"), 10, Category.ELECTRONICS);
        Product book   = new Product(3L, "Book",   "Clean Code",    new BigDecimal("40.00"),  50, Category.BOOKS);

        ShoppingCart cart = new ShoppingCart(new RegularPricing());

        // addItem
        cart.addItem(phone, 2);
        cart.addItem(laptop, 1);
        cart.addItem(book, 3);
        System.out.println("\n  -- After adding items (RegularPricing) --");
        cart.getItems().values().forEach(i -> System.out.println("  " + i));
        System.out.println("  Total: $" + cart.getTotalAmount());

        // addItem same product — merges quantity
        cart.addItem(phone, 1); // phone qty: 2 → 3
        System.out.println("\n  -- After addItem(phone, 1) again — qty merges to 3 --");
        System.out.println("  " + cart.getItems().get(1L));

        // updateQuantity
        cart.updateQuantity(3L, 10); // book qty: 3 → 10
        System.out.println("\n  -- After updateQuantity(book, 10) --");
        System.out.println("  " + cart.getItems().get(3L));

        // removeItem
        cart.removeItem(2L); // remove laptop
        System.out.println("\n  -- After removeItem(laptop) --");
        cart.getItems().values().forEach(i -> System.out.println("  " + i));

        // strategy swap — same cart, different pricing
        System.out.println("\n  -- Strategy swap --");
        System.out.println("  Regular total:  $" + cart.getTotalAmount());
        cart.setPricingStrategy(new PremiumUserPricing());
        System.out.println("  Premium total:  $" + cart.getTotalAmount() + " (20% off)");
        cart.setPricingStrategy(new BulkPricing());
        System.out.println("  Bulk total:     $" + cart.getTotalAmount() + " (10% off if qty>=10)");
    }
}
