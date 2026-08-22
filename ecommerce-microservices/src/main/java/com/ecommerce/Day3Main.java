package com.ecommerce;

import com.ecommerce.domain.enums.Category;
import com.ecommerce.domain.exception.*;
import com.ecommerce.domain.model.Product;
import com.ecommerce.domain.pattern.factory.NotificationFactory;
import com.ecommerce.domain.pattern.observer.*;
import com.ecommerce.domain.pattern.singleton.IdGenerator;
import com.ecommerce.domain.pattern.strategy.*;

import java.math.BigDecimal;

public class Day3Main {

    public static void main(String[] args) {
        testPricingStrategies();
        testStrategySwapAtRuntime();
        testObserver();
        testExceptions();
        testNotificationFactory();
        testSingleton();
    }

    private static void testPricingStrategies() {
        System.out.println("=== Strategy: Pricing ===");

        Product laptop = new Product(1L, "Laptop", "Gaming Laptop", new BigDecimal("1000.00"), 50, Category.ELECTRONICS);
        Product book   = new Product(2L, "Java Book", "Effective Java", new BigDecimal("50.00"), 100, Category.BOOKS);

        PricingStrategy regular = new RegularPricing();
        PricingStrategy bulk    = new BulkPricing();
        PricingStrategy premium = new PremiumUserPricing();

        int smallQty = 5;
        int bulkQty  = 15;

        System.out.println("\n  -- Laptop ($1000) --");
        System.out.println("  Regular  (qty=" + smallQty + "): $" + regular.calculatePrice(laptop, smallQty));
        System.out.println("  Bulk     (qty=" + smallQty + "): $" + bulk.calculatePrice(laptop, smallQty) + " (no discount, below threshold of 10)");
        System.out.println("  Bulk     (qty=" + bulkQty  + "): $" + bulk.calculatePrice(laptop, bulkQty)  + " (10% off)");
        System.out.println("  Premium  (qty=" + smallQty + "): $" + premium.calculatePrice(laptop, smallQty) + " (20% off)");

        System.out.println("\n  -- Java Book ($50) --");
        System.out.println("  Regular  (qty=" + bulkQty + "): $" + regular.calculatePrice(book, bulkQty));
        System.out.println("  Bulk     (qty=" + bulkQty + "): $" + bulk.calculatePrice(book, bulkQty)    + " (10% off)");
        System.out.println("  Premium  (qty=" + bulkQty + "): $" + premium.calculatePrice(book, bulkQty) + " (20% off)");
    }

    private static void testStrategySwapAtRuntime() {
        System.out.println("\n=== Strategy Swap at Runtime ===");

        Product phone = new Product(3L, "Phone", "Smartphone", new BigDecimal("800.00"), 30, Category.ELECTRONICS);

        // start as regular user
        ShoppingCart cart = new ShoppingCart(new RegularPricing());
        cart.addItem(phone, 2);
        System.out.println("  Regular total:  $" + cart.getTotalAmount());

        // user upgrades to premium — swap strategy, cart code unchanged
        cart.setPricingStrategy(new PremiumUserPricing());
        System.out.println("  Premium total:  $" + cart.getTotalAmount() + " (20% off)");

        // bulk order
        cart.setPricingStrategy(new BulkPricing());
        System.out.println("  Bulk total:     $" + cart.getTotalAmount() + " (no discount, qty=2 below 10)");
    }

    private static void testObserver() {
        System.out.println("\n=== Observer: EventPublisher ===");

        EventPublisher publisher = new InMemoryEventPublisher();

        // lambdas work because EventListener is @FunctionalInterface
        // multiple listeners on the same event — all fire when ORDER_PLACED is published
        publisher.subscribe(EventType.ORDER_PLACED,    data -> System.out.println("  [EmailService]     Order confirmation sent    | " + data));
        publisher.subscribe(EventType.ORDER_PLACED,    data -> System.out.println("  [SMSService]       Order SMS alert sent       | " + data));
        publisher.subscribe(EventType.ORDER_PLACED,    data -> System.out.println("  [InventoryService] Stock reserved              | " + data));
        publisher.subscribe(EventType.PAYMENT_FAILED,  data -> System.out.println("  [AlertService]     Payment failure alert sent  | " + data));
        publisher.subscribe(EventType.USER_REGISTERED, data -> System.out.println("  [EmailService]     Welcome email sent         | " + data));

        System.out.println("\n  -- Publishing ORDER_PLACED --");
        publisher.publish(EventType.ORDER_PLACED, "orderId=101, userId=1, total=$890.00");

        System.out.println("\n  -- Publishing PAYMENT_FAILED --");
        publisher.publish(EventType.PAYMENT_FAILED, "orderId=102, reason=card declined");

        // no listeners registered for ORDER_SHIPPED — nothing fires, no crash
        System.out.println("\n  -- Publishing ORDER_SHIPPED (no listeners) --");
        publisher.publish(EventType.ORDER_SHIPPED, "orderId=101");
        System.out.println("  (no output — nobody subscribed to ORDER_SHIPPED)");

        // unsubscribe all ORDER_PLACED listeners and verify nothing fires
        System.out.println("\n  -- After unsubscribeAll(ORDER_PLACED) --");
        publisher.unsubscribeAll(EventType.ORDER_PLACED);
        publisher.publish(EventType.ORDER_PLACED, "orderId=103");
        System.out.println("  (no output — all ORDER_PLACED listeners removed)");
    }

    private static void testExceptions() {
        System.out.println("\n=== Exceptions ===");

        // ProductNotFoundException — throw, catch, print errorCode
        try {
            throw new ProductNotFoundException(42L);
        } catch (EcommerceException e) {
            System.out.println("  Caught: " + e);                   // [PRODUCT_001] Product not found with id: 42
            System.out.println("  errorCode: " + e.getErrorCode()); // PRODUCT_001
        }

        // InsufficientStockException
        try {
            throw new InsufficientStockException(7L, 50, 3);
        } catch (EcommerceException e) {
            System.out.println("  Caught: " + e);                   // [PRODUCT_002] Insufficient stock...
            System.out.println("  errorCode: " + e.getErrorCode()); // PRODUCT_002
        }

        // InvalidOrderStateException
        try {
            throw new InvalidOrderStateException("Cannot transition from DELIVERED to PENDING");
        } catch (EcommerceException e) {
            System.out.println("  Caught: " + e);                   // [ORDER_001] Cannot transition...
            System.out.println("  errorCode: " + e.getErrorCode()); // ORDER_001
        }

        // PaymentFailedException
        try {
            throw new PaymentFailedException("card declined");
        } catch (EcommerceException e) {
            System.out.println("  Caught: " + e);                   // [PAYMENT_001] Payment failed: card declined
        }

        // DuplicateEmailException
        try {
            throw new DuplicateEmailException("user@example.com");
        } catch (EcommerceException e) {
            System.out.println("  Caught: " + e);                   // [USER_001] Email already registered...
        }

        // UnauthorizedException
        try {
            throw new UnauthorizedException("DELETE_PRODUCT");
        } catch (EcommerceException e) {
            System.out.println("  Caught: " + e);                   // [AUTH_001] Unauthorized to perform action...
        }

        // All are unchecked — catching the base EcommerceException catches all subtypes
        System.out.println("  All exceptions caught via base EcommerceException — polymorphism at work");
    }

    private static void testNotificationFactory() {
        System.out.println("\n=== Factory: NotificationFactory ===");

        // Each EventType maps to a distinct message template
        for (EventType type : EventType.values()) {
            System.out.println("  " + type + " -> " + NotificationFactory.create(type));
        }
    }

    private static void testSingleton() {
        System.out.println("\n=== Singleton: IdGenerator ===");

        IdGenerator g1 = IdGenerator.getInstance();
        IdGenerator g2 = IdGenerator.getInstance();

        // Both references point to the same instance — singleton guarantee
        System.out.println("  Same instance: " + (g1 == g2));      // true

        // getId() increments atomically — thread-safe via AtomicLong
        System.out.println("  id1: " + g1.getId());                 // 1
        System.out.println("  id2: " + g1.getId());                 // 2
        System.out.println("  id3 via g2: " + g2.getId());          // 3 — same counter, same instance
    }
}
