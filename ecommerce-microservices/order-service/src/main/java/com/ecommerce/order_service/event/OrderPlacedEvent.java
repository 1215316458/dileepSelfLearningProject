package com.ecommerce.order_service.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * OrderPlacedEvent — published to Kafka topic "order-placed" when an order is created.
 *
 * Design decisions:
 * - Record (immutable) — events should never be mutated after creation
 * - eventId — used by consumers for idempotency (skip if already processed)
 * - Instant timestamp — UTC, timezone-safe
 * - totalAmount included so notification-service doesn't need to call order-service back
 *   (self-contained event = no chatty follow-up calls)
 */
public record OrderPlacedEvent(
        String    eventId,      // UUID — for idempotency deduplication
        Long      orderId,
        Long      userId,
        BigDecimal totalAmount,
        String    shippingAddress,
        Instant   occurredAt
) {
    public static OrderPlacedEvent of(Long orderId, Long userId,
                                      BigDecimal totalAmount, String shippingAddress) {
        return new OrderPlacedEvent(
                java.util.UUID.randomUUID().toString(),
                orderId, userId, totalAmount, shippingAddress,
                Instant.now()
        );
    }
}
