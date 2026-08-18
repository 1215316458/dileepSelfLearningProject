package com.ecommerce;

import com.ecommerce.domain.enums.Category;
import com.ecommerce.domain.model.Product;
import com.ecommerce.domain.pattern.observer.*;
import com.ecommerce.domain.pattern.strategy.*;

import java.math.BigDecimal;

public class Day3Main {

    public static void main(String[] args) {
        testPricingStrategies();
        testStrategySwapAtRuntime();
        testObserver();
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
        System.out.println("  Regular total:  $" + cart.calculateTotal());

        // user upgrades to premium — swap strategy, cart code unchanged
        cart.setPricingStrategy(new PremiumUserPricing());
        System.out.println("  Premium total:  $" + cart.calculateTotal() + " (20% off)");

        // bulk order
        cart.setPricingStrategy(new BulkPricing());
        System.out.println("  Bulk total:     $" + cart.calculateTotal() + " (no discount, qty=2 below 10)");
    }

    private static void testObserver() {
        System.out.println("\n=== Observer: EventPublisher ===");

        EventPublisher publisher = new InMemoryEventPublisher();

        // lambdas work because EventListener is @FunctionalInterface
        // multiple listeners on the same event — all fire when ORDER_PLACED is published
        publisher.subscribe(EventType.ORDER_PLACED,   data -> System.out.println("  [EmailService]     Order confirmation sent    | " + data));
        publisher.subscribe(EventType.ORDER_PLACED,   data -> System.out.println("  [SMSService]       Order SMS alert sent       | " + data));
        publisher.subscribe(EventType.ORDER_PLACED,   data -> System.out.println("  [InventoryService] Stock reserved              | " + data));
        publisher.subscribe(EventType.PAYMENT_FAILED, data -> System.out.println("  [AlertService]     Payment failure alert sent  | " + data));
        publisher.subscribe(EventType.USER_REGISTERED,data -> System.out.println("  [EmailService]     Welcome email sent         | " + data));

        // publish — fires all listeners registered for ORDER_PLACED
        System.out.println("\n  -- Publishing ORDER_PLACED --");
        publisher.publish(EventType.ORDER_PLACED, "orderId=101, userId=1, total=$890.00");

        // only AlertService fires here
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
}
