package com.ecommerce.notification_service.event;

import com.ecommerce.notification_service.domain.NotificationType;
import com.ecommerce.notification_service.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OrderEventConsumer — listens to Kafka topics and creates notifications.
 *
 * Consumer group "notification-service":
 * - All instances of notification-service share this group
 * - Kafka assigns each partition to exactly one instance in the group
 * - If one instance dies, Kafka rebalances partitions to surviving instances
 *
 * Idempotency:
 * - Kafka guarantees at-least-once delivery — the same event CAN arrive twice
 *   (e.g. consumer crashes after processing but before committing offset)
 * - We track processed eventIds in a Set to skip duplicates
 * - In production: use Redis or a DB table for persistent idempotency tracking
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    // In-memory idempotency store — replace with Redis in production
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    private final NotificationService notificationService;

    public OrderEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * @KafkaListener:
     * - topics: the Kafka topic to subscribe to
     * - groupId: consumer group — all instances share the load
     * - containerFactory: uses the JsonDeserializer configured in application.yml
     *
     * @Payload: the deserialized event object
     * @Header(KafkaHeaders.RECEIVED_PARTITION): which partition this message came from
     * @Header(KafkaHeaders.OFFSET): the offset within that partition
     */
    @KafkaListener(
            topics = "order-placed",
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderPlaced(
            @Payload OrderPlacedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received OrderPlacedEvent orderId={} from partition={} offset={}",
                event.orderId(), partition, offset);

        // Idempotency check — skip if we already processed this event
        if (!processedEventIds.add(event.eventId())) {
            log.warn("Duplicate event skipped: eventId={}", event.eventId());
            return;
        }

        try {
            notificationService.createOrderNotification(
                    event.userId(), event.orderId(), NotificationType.ORDER_PLACED);
            log.info("Notification created for userId={} orderId={}", event.userId(), event.orderId());
        } catch (Exception e) {
            // Remove from processed set so it can be retried
            processedEventIds.remove(event.eventId());
            log.error("Failed to create notification for orderId={}: {}", event.orderId(), e.getMessage());
            throw e; // re-throw so Kafka retries (or sends to dead letter topic)
        }
    }
}
