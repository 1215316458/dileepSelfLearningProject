package com.ecommerce.domain.pattern.factory;

import com.ecommerce.domain.pattern.observer.EventType;

// Simple Factory — maps an EventType to a human-readable notification template.
// Keeps message formatting logic in one place; callers just pass the event type.
public class NotificationFactory {

    private NotificationFactory() {}

    public static String create(EventType eventType) {
        switch (eventType) {
            case ORDER_PLACED:      return "Your order has been placed successfully. Order ID: {orderId}";
            case ORDER_CANCELLED:   return "Your order {orderId} has been cancelled.";
            case ORDER_SHIPPED:     return "Great news! Your order {orderId} has been shipped.";
            case ORDER_DELIVERED:   return "Your order {orderId} has been delivered. Enjoy!";
            case PAYMENT_PROCESSED: return "Payment of {amount} processed successfully for order {orderId}.";
            case PAYMENT_FAILED:    return "Payment failed for order {orderId}. Please retry or use a different method.";
            case STOCK_LOW:         return "Stock alert: product {productId} is running low ({stock} units remaining).";
            case USER_REGISTERED:   return "Welcome! Your account has been created successfully.";
            default:                throw new IllegalArgumentException("No template for event type: " + eventType);
        }
    }
}
