# Day 25 — Kafka: Producers + Consumers

## What We Built

- `OrderPlacedEvent` record — published by order-service when an order is created
- `OrderEventPublisher` — wraps `KafkaTemplate`, publishes to `order-placed` topic
- `OrderEventConsumer` — `@KafkaListener` in notification-service, creates a `Notification` document
- Idempotency tracking — `ConcurrentHashMap` set of processed `eventId`s
- Kafka config in both service `application.yml` files

**Tests: 25/25 order-service, 18/18 notification-service** (Kafka mocked in tests)

---

## Why Kafka?

Before Kafka, order-service called notification-service directly (HTTP):
```
order-service → POST http://notification-service/api/notifications
```

Problems:
- Tight coupling — order-service must know notification-service exists
- If notification-service is down, the order fails
- If notification-service is slow, the order response is slow
- Hard to add more consumers (e.g. analytics-service also wants order events)

With Kafka:
```
order-service → publishes OrderPlacedEvent to Kafka topic "order-placed"
notification-service → consumes from "order-placed" → creates notification
product-service → consumes from "order-placed" → reserves stock (Day 26)
analytics-service → consumes from "order-placed" → updates dashboards
```

Order-service doesn't know or care who consumes the event. Adding a new consumer
requires zero changes to order-service.

---

## Core Concepts

### Topic

A named, ordered, append-only log. Like a database table but for events.
- `order-placed` — all order placement events
- `stock-reserved` — stock reservation confirmations
- `payment-processed` — payment results

### Partition

Each topic is split into partitions. Partitions enable parallelism:
- 3 partitions → 3 consumers can read in parallel
- Messages with the same key always go to the same partition (ordering guarantee)

We use `orderId.toString()` as the key → all events for the same order go to the
same partition → they're processed in order.

### Consumer Group

A group of consumers that share the work of consuming a topic:
- `notification-service` group has 2 instances → each reads different partitions
- If one instance dies, Kafka rebalances its partitions to the other instance

### Offset

Each message has an offset (position) within its partition. Consumers commit their
offset after processing. If a consumer crashes and restarts, it resumes from the
last committed offset.

---

## Producer (order-service)

```java
@Component
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderPlaced(OrderPlacedEvent event) {
        kafkaTemplate.send("order-placed", event.orderId().toString(), event);
    }
}
```

`KafkaTemplate.send()` is async — returns `CompletableFuture`. We attach callbacks
to log success/failure without blocking the HTTP response.

### Producer Config

```yaml
spring:
  kafka:
    producer:
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all      # wait for all in-sync replicas to acknowledge
      retries: 3
```

`acks: all` — strongest durability. The producer waits for the leader AND all
in-sync replicas to write the message before returning success. No data loss even
if the leader crashes immediately after.

---

## Consumer (notification-service)

```java
@KafkaListener(
    topics = "order-placed",
    groupId = "notification-service"
)
public void onOrderPlaced(@Payload OrderPlacedEvent event,
                          @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                          @Header(KafkaHeaders.OFFSET) long offset) {

    if (!processedEventIds.add(event.eventId())) {
        return;  // duplicate — skip
    }

    notificationService.createOrderNotification(
        event.userId(), event.orderId(), NotificationType.ORDER_PLACED);
}
```

### Consumer Config

```yaml
spring:
  kafka:
    consumer:
      auto-offset-reset: earliest   # read from beginning if no committed offset
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.ecommerce.notification_service.event"
        spring.json.value.default.type: "...OrderPlacedEvent"
```

`auto-offset-reset: earliest` — in dev, if the consumer starts after messages were
published, it reads from the beginning. In production, use `latest` to only process
new messages.

---

## Idempotency

Kafka guarantees **at-least-once delivery** — the same message CAN arrive twice:
1. Consumer processes the message
2. Consumer crashes before committing the offset
3. Consumer restarts, reads the same message again

Without idempotency: duplicate notifications sent to the user.

Fix: track processed `eventId`s:
```java
private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

if (!processedEventIds.add(event.eventId())) {
    return;  // already processed
}
```

In production: use Redis or a DB table (in-memory set is lost on restart).

---

## Event Design

```java
public record OrderPlacedEvent(
    String    eventId,       // UUID — for idempotency
    Long      orderId,
    Long      userId,
    BigDecimal totalAmount,  // self-contained — no follow-up calls needed
    String    shippingAddress,
    Instant   occurredAt     // UTC timestamp
) {}
```

**Self-contained events** — include all data consumers need. If notification-service
had to call order-service to get the total amount, that's a chatty follow-up call
that defeats the purpose of async messaging.

**Naming convention**: `noun-verb` past tense (`order-placed`, not `place-order`).
Events describe what happened, not what to do.

---

## Testing Without Kafka

Kafka is excluded in the test profile:
```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
```

`OrderEventPublisher` and `OrderMetrics` are mocked in all Spring context tests:
```java
@MockitoBean
OrderEventPublisher eventPublisher;

@MockitoBean
OrderMetrics metrics;
```

The `Timer.record(Supplier)` call in `placeOrder` is stubbed to actually invoke
the supplier so the business logic runs:
```java
Timer mockTimer = mock(Timer.class);
when(metrics.orderProcessingTimer()).thenReturn(mockTimer);
doAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get())
        .when(mockTimer).record(any(Supplier.class));
```

---

## pom.xml Changes (Day 25)

Added to order-service and notification-service:
```xml
<!-- spring-kafka 3.1.0 + kafka-clients 3.6.0 are cached -->
<!-- SB4.1.1 BOM manages spring-kafka at a newer version, so we pin -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
    <version>3.1.0</version>
</dependency>
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.6.0</version>
</dependency>
```

---

## Spring Boot 4.x Breaking Changes (Day 25)

| Issue | Cause | Fix |
|-------|-------|-----|
| `KafkaTemplate` not available in tests | `KafkaAutoConfiguration` excluded in test profile | `@MockitoBean OrderEventPublisher` in all Spring context tests |
| `Timer.record(Supplier)` NPE in tests | `OrderMetrics` is mocked, `orderProcessingTimer()` returns null | Stub `orderProcessingTimer()` to return a mock Timer, stub `record()` to invoke the Supplier |
| Duplicate `spring:` key in application.yml | Kafka config added as second `spring:` root block | Merge `spring.kafka` into the existing `spring:` block |
