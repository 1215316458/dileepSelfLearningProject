package com.ecommerce;

import com.ecommerce.domain.enums.Category;
import com.ecommerce.domain.enums.OrderStatus;
import com.ecommerce.domain.enums.Role;
import com.ecommerce.domain.exception.InvalidOrderStateException;
import com.ecommerce.domain.model.*;

import java.math.BigDecimal;

public class Day2Main {

    public static void main(String[] args) {
        testDomainModels();
        testOrderBuilder();
        testOrderTransitions();
    }

    private static void testDomainModels() {
        System.out.println("=== Domain Models ===");

        Product laptop = new Product(1L, "Laptop", "Gaming laptop", new BigDecimal("1200.00"), 10, Category.ELECTRONICS);
        Product shirt  = new Product(2L, "Shirt",  "Cotton shirt",  new BigDecimal("25.00"),   50, Category.CLOTHING);
        User user      = new User(1L, "john_doe", "john@example.com", "secret123", Role.CUSTOMER);

        System.out.println("  " + laptop);
        System.out.println("  " + shirt);
        System.out.println("  " + user);

        CartItem item1 = new CartItem(laptop, 2);
        CartItem item2 = new CartItem(shirt, 3);
        System.out.println("  " + item1 + " | subtotal: " + item1.getSubtotal());
        System.out.println("  " + item2 + " | subtotal: " + item2.getSubtotal());
    }

    private static void testOrderBuilder() {
        System.out.println("\n=== Order Builder ===");

        Product phone = new Product(3L, "Phone", "Smartphone", new BigDecimal("800.00"), 20, Category.ELECTRONICS);
        Product book  = new Product(4L, "Java Book", "Effective Java", new BigDecimal("45.00"), 100, Category.BOOKS);

        Order order = new Order.Builder()
                .id(1L)
                .userId(1L)
                .addItem(new CartItem(phone, 1))
                .addItem(new CartItem(book, 2))
                .shippingAddress("123 Main St, Springfield")
                .build();

        System.out.println("  Built: " + order);
        System.out.println("  Total: " + order.getTotalAmount()); // 800 + (45*2) = 890.00
        System.out.println("  Items: " + order.getItems());
    }

    private static void testOrderTransitions() {
        System.out.println("\n=== Order Transitions ===");

        Product item = new Product(5L, "Headphones", "Wireless", new BigDecimal("150.00"), 30, Category.ELECTRONICS);
        Order order = new Order.Builder()
                .id(2L)
                .userId(2L)
                .addItem(new CartItem(item, 1))
                .build();

        // valid transitions
        tryTransition(order, OrderStatus.CONFIRMED);
        tryTransition(order, OrderStatus.SHIPPED);
        tryTransition(order, OrderStatus.DELIVERED);

        // invalid transition — DELIVERED is a terminal state
        tryTransition(order, OrderStatus.CANCELLED);
    }

    private static void tryTransition(Order order, OrderStatus next) {
        try {
            OrderStatus before = order.getStatus();
            order.transitionTo(next);
            System.out.println("  ✓ " + before + " → " + order.getStatus());
        } catch (InvalidOrderStateException e) {
            System.out.println("  ✗ Failed: " + e.getMessage());
        }
    }
}
