# Day 26 — Saga Choreography + CQRS + Dead Letter

## What We Built (Conceptual — no new code)

Day 26 extends the Kafka foundation from Day 25 with architectural patterns.
The concepts are documented here; full implementation requires a running Kafka broker.

---

## Saga Pattern

A **saga** is a sequence of local transactions, each publishing an event that
triggers the next step. If any step fails, compensating transactions undo the
previous steps.

### Choreography vs Orchestration

**Choreography** (what we use):
- No central coordinator
- Each service reacts to events and publishes new events
- Services are fully decoupled — they don't know about each other

```
order-service    → publishes OrderPlacedEvent
product-service  → consumes OrderPlacedEvent → reserves stock → publishes StockReservedEvent
payment-service  → consumes StockReservedEvent → charges card → publishes PaymentProcessedEvent
order-service    → consumes PaymentProcessedEvent → confirms order → publishes OrderConfirmedEvent
notification-service → consumes OrderConfirmedEvent → sends confirmation notification
```

**Orchestration** (alternative):
- A central saga orchestrator calls each service in sequence
- Easier to understand the flow, but creates a central point of coupling

### Compensation (Rollback)

If payment fails:
```
payment-service → publishes PaymentFailedEvent
product-service → consumes PaymentFailedEvent → releases reserved stock → publishes StockReleasedEvent
order-service   → consumes StockReleasedEvent → cancels order → publishes OrderCancelledEvent
notification-service → consumes OrderCancelledEvent → sends cancellation notification
```

Each step has a compensating action. The saga guarantees eventual consistency —
not ACID atomicity. There's a window where the order exists but stock isn't reserved.

### Full Event Flow

```
OrderPlacedEvent
    ↓
StockReservedEvent ──────────────────────────────→ StockReservationFailedEvent
    ↓                                                       ↓
PaymentProcessedEvent ──────────────→ PaymentFailedEvent   OrderCancelledEvent
    ↓                                       ↓
OrderConfirmedEvent              StockReleasedEvent
    ↓
(notification sent)
```

---

## Dead Letter Queue

When a consumer fails to process a message after N retries, Kafka sends it to a
**dead letter topic** (DLT):

```yaml
spring:
  kafka:
    consumer:
      properties:
        spring.kafka.listener.ack-mode: MANUAL
```

```java
@KafkaListener(topics = "order-placed")
public void onOrderPlaced(OrderPlacedEvent event) {
    try {
        process(event);
    } catch (Exception e) {
        // After 3 retries, Spring Kafka sends to "order-placed.DLT"
        throw e;
    }
}

@KafkaListener(topics = "order-placed.DLT")
public void onDeadLetter(OrderPlacedEvent event) {
    // Alert, log, manual intervention
    log.error("Dead letter: {}", event);
}
```

Why DLT matters:
- Without it, a poison pill message (one that always fails) blocks the entire partition
- With DLT, the bad message is moved aside and processing continues
- Operations team can inspect and replay DLT messages after fixing the bug

---

## CQRS — Command Query Responsibility Segregation

**Write side** (Command): order-service writes to MySQL
```
POST /api/orders → OrderService.placeOrder() → H2/MySQL
```

**Read side** (Query): notification-service reads from MongoDB
```
GET /api/notifications/user/1 → NotificationService.getByUser() → MongoDB
```

The two sides use different data stores optimised for their purpose:
- MySQL: ACID transactions, relational queries, strong consistency
- MongoDB: flexible schema, fast reads, horizontal scaling

They stay in sync via Kafka events:
```
order-service writes to MySQL
    → publishes OrderPlacedEvent to Kafka
    → notification-service consumes event
    → writes to MongoDB
```

This is **eventual consistency** — MongoDB may be a few milliseconds behind MySQL.
For notifications, that's acceptable.

### Why CQRS?

Without CQRS, you'd query MySQL for notifications — but MySQL is optimised for
transactional writes, not for "give me all notifications for user 1 sorted by date."
MongoDB's document model is a better fit for that query pattern.

---

## Event Sourcing (Concept)

Instead of storing the current state, store every event that led to the current state:

```
OrderCreated { userId: 1, items: [...] }
OrderConfirmed { orderId: 100 }
OrderShipped { orderId: 100, trackingNumber: "ABC123" }
```

To get the current order state: replay all events.

Benefits:
- Complete audit trail — you know exactly what happened and when
- Time travel — reconstruct state at any point in time
- Event replay — rebuild read models from scratch

Drawbacks:
- More complex to implement
- Queries require replaying events (mitigated by snapshots)
- Schema evolution is harder

We don't implement event sourcing here — it's a significant architectural commitment.
The Kafka event log gives us some of the benefits (audit trail) without full event sourcing.

---

## Backpressure

If notification-service is slow, Kafka messages pile up. Control this with:

```yaml
spring:
  kafka:
    consumer:
      properties:
        max.poll.records: 10   # process 10 messages per poll (default 500)
```

This limits how many messages the consumer fetches at once, giving it time to
process each batch before fetching more.
