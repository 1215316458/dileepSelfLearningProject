package com.ecommerce.notification_service.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Mirror of order-service's OrderPlacedEvent.
 * In a real system this would be a shared library (common-events module).
 * Here we duplicate it to keep services independent — no compile-time coupling.
 */
public record OrderPlacedEvent(
        String     eventId,
        Long       orderId,
        Long       userId,
        BigDecimal totalAmount,
        String     shippingAddress,
        Instant    occurredAt
) {}
