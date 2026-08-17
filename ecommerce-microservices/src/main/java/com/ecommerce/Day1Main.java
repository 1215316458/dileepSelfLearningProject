package com.ecommerce;

import com.ecommerce.domain.enums.Category;
import com.ecommerce.domain.enums.OrderStatus;
import com.ecommerce.domain.enums.Role;

import java.util.Optional;

public class Day1Main {

    public static void main(String[] args) {
        testOrderStatusTransitions();
        testCategoryFromString();
        testRoles();
    }

    private static void testOrderStatusTransitions() {
        System.out.println("=== OrderStatus Transitions ===");

        // valid transitions
        tryTransition(OrderStatus.PENDING, OrderStatus.CONFIRMED);
        tryTransition(OrderStatus.CONFIRMED, OrderStatus.SHIPPED);
        tryTransition(OrderStatus.SHIPPED, OrderStatus.DELIVERED);

        // invalid transitions
        tryTransition(OrderStatus.PENDING, OrderStatus.DELIVERED);  // can't skip steps
        tryTransition(OrderStatus.DELIVERED, OrderStatus.CANCELLED); // terminal state
        tryTransition(OrderStatus.CANCELLED, OrderStatus.CONFIRMED); // terminal state
    }

    private static void tryTransition(OrderStatus from, OrderStatus to) {
        if (from.canTransitionTo(to)) {
            System.out.println("  ✓ " + from + " → " + to);
        } else {
            System.out.println("  ✗ " + from + " → " + to + " [INVALID] allowed: " + from.allowedTransitions());
        }
    }

    private static void testCategoryFromString() {
        System.out.println("\n=== Category.fromString ===");

        String[] inputs = {"electronics", "BOOKS", "clothing", "invalid", null};
        for (String input : inputs) {
            Optional<Category> result = Category.fromString(input);
            if (result.isPresent()) {
                Category c = result.get();
                System.out.println("  ✓ \"" + input + "\" → " + c + " | tax: " + c.getTaxRate());
            } else {
                System.out.println("  ✗ \"" + input + "\" → not found");
            }
        }
    }

    private static void testRoles() {
        System.out.println("\n=== Roles ===");
        for (Role role : Role.values()) {
            System.out.println("  " + role.name() + " → displayName: " + role.getDisplayName());
        }
    }
}
