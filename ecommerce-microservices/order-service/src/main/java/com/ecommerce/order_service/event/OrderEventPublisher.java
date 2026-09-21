package com.ecommerce.order_service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * OrderEventPublisher — wraps KafkaTemplate to publish order domain events.
 *
 * KafkaTemplate.send() is async — it returns a CompletableFuture.
 * We attach callbacks to log success/failure without blocking the caller.
 *
 * Topic naming convention: noun-verb (order-placed, stock-reserved)
 * This makes it clear what happened, not what to do (avoid: process-order).
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);
    public static final String ORDER_PLACED_TOPIC = "order-placed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publish OrderPlacedEvent.
     * Key = orderId.toString() — ensures all events for the same order
     * go to the same partition (ordering guarantee per order).
     */
    public void publishOrderPlaced(OrderPlacedEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(ORDER_PLACED_TOPIC, event.orderId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish OrderPlacedEvent orderId={}: {}",
                        event.orderId(), ex.getMessage());
            } else {
                log.info("Published OrderPlacedEvent orderId={} to partition={} offset={}",
                        event.orderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
