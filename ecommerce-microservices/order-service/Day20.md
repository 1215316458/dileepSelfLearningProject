# Day 20 — Order Service: Transactions + Async + Saga

## What was built (added to order-service)

| File | Purpose |
|---|---|
| `service/OrderService.java` | `@Transactional` with propagation/isolation, `@Async`, `@Scheduled` |
| `service/AuditLogService.java` | `REQUIRES_NEW` propagation demo |
| `service/SagaOrchestrator.java` | Saga pattern with compensation steps |
| `config/AsyncConfig.java` | `@EnableAsync` + custom `ThreadPoolTaskExecutor` |

All 14 tests continue to pass.

---

## 1. @Transactional — The Full Picture

### What it does
`@Transactional` wraps a method in a database transaction. If the method completes normally, the transaction commits. If it throws a `RuntimeException` (or any exception configured with `rollbackFor`), the transaction rolls back — all DB changes are undone atomically.

Spring implements `@Transactional` with AOP: the bean is wrapped in a proxy that begins/commits/rolls back the transaction around the method call.

```
Caller → AOP Proxy (begin transaction) → real method → AOP Proxy (commit or rollback)
```

**Important**: `@Transactional` only works when called through the proxy. Calling a `@Transactional` method from within the same class bypasses the proxy and has no transaction effect.

### readOnly = true

```java
@Transactional(readOnly = true)
public OrderResponse getOrder(Long id) { ... }
```

`readOnly = true` tells Hibernate to skip dirty checking (comparing entity state to detect changes) and tells the DB driver it can use a read replica. It doesn't prevent writes — it's a hint for optimization.

### Default rollback behavior
- Rolls back on: `RuntimeException` and `Error`
- Does NOT roll back on: checked exceptions (unless `rollbackFor` is specified)

```java
@Transactional(rollbackFor = Exception.class)  // roll back on checked exceptions too
```

---

## 2. Transaction Propagation

Propagation defines what happens when a `@Transactional` method is called from within another `@Transactional` method.

### REQUIRED (default)
```java
@Transactional  // propagation = REQUIRED by default
public OrderResponse placeOrder(PlaceOrderRequest req) {
    Order saved = orderRepository.save(order);
    auditLogService.logOrderCreated(saved.getId(), saved.getUserId()); // joins this transaction
}
```
If a transaction already exists, join it. If not, create a new one. The audit log call joins the outer transaction — if `placeOrder` rolls back, the audit log is also rolled back.

### REQUIRES_NEW
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void logOrderCreated(Long orderId, Long userId) {
    // runs in its own independent transaction
}
```
Suspends the current transaction and starts a fresh one. This transaction commits independently. Even if the outer `placeOrder` transaction rolls back, the audit log entry is preserved.

**Use case**: audit logs, notification records — things that must be persisted even when the business transaction fails.

### Other propagation types

| Propagation | Behavior |
|---|---|
| `REQUIRED` | Join existing or create new (default) |
| `REQUIRES_NEW` | Always create new, suspend existing |
| `SUPPORTS` | Join if exists, run without transaction if not |
| `NOT_SUPPORTED` | Always run without transaction, suspend existing |
| `MANDATORY` | Must have existing transaction, throw if none |
| `NEVER` | Must NOT have transaction, throw if one exists |
| `NESTED` | Nested transaction (savepoint) within existing |

---

## 3. Transaction Isolation

Isolation defines how much a transaction is shielded from concurrent transactions.

### The concurrency problems isolation solves

**Dirty read**: Transaction A reads data written by Transaction B before B commits. If B rolls back, A read invalid data.

**Non-repeatable read**: Transaction A reads a row, Transaction B updates it and commits, Transaction A reads the same row again and gets different data.

**Phantom read**: Transaction A queries rows matching a condition, Transaction B inserts a new matching row and commits, Transaction A re-queries and sees the new row.

### Isolation levels

| Level | Dirty Read | Non-Repeatable Read | Phantom Read |
|---|---|---|---|
| `READ_UNCOMMITTED` | Possible | Possible | Possible |
| `READ_COMMITTED` | Prevented | Possible | Possible |
| `REPEATABLE_READ` | Prevented | Prevented | Possible |
| `SERIALIZABLE` | Prevented | Prevented | Prevented |

```java
// Stock check — REPEATABLE_READ prevents another transaction from
// changing the stock between our read and the order save
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public boolean checkStock(Long productId, int required) {
    ProductResponse product = getProduct(productId);
    return product.stock() >= required;
}
```

**Default isolation** in most databases (including MySQL) is `READ_COMMITTED`. Spring's default is `DEFAULT` — uses the database's default.

**Performance trade-off**: higher isolation = more locking = lower throughput. `SERIALIZABLE` is rarely used in practice.

---

## 4. @Async — Non-Blocking Operations

```java
@Async("orderExecutor")
public CompletableFuture<Void> sendConfirmationAsync(Long orderId, Long userId) {
    // runs in a separate thread from the "orderExecutor" pool
    log.info("Sending confirmation for order {}", orderId);
    return CompletableFuture.completedFuture(null);
}
```

### How it works
`@EnableAsync` on `AsyncConfig` activates Spring's async method execution. `@Async` methods are wrapped in an AOP proxy that submits the method to an `Executor` instead of running it on the caller's thread.

```
HTTP request thread → placeOrder() → save order → sendConfirmationAsync() → returns immediately
                                                         ↓
                                              order-async-1 thread → sends email
```

The HTTP response is returned before the email is sent. The user doesn't wait for the email.

### Why CompletableFuture?
- `void` return: fire-and-forget, no way to check result
- `CompletableFuture<Void>`: caller can chain `.thenRun()`, `.exceptionally()`, or ignore it
- `CompletableFuture<T>`: caller can get the result asynchronously

### Custom executor
```java
@Bean("orderExecutor")
public Executor orderExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);      // always-alive threads
    executor.setMaxPoolSize(5);       // max threads under load
    executor.setQueueCapacity(100);   // tasks queued when all threads busy
    executor.setThreadNamePrefix("order-async-");
    executor.setWaitForTasksToCompleteOnShutdown(true);  // graceful shutdown
    executor.setAwaitTerminationSeconds(30);
    return executor;
}
```

Without a custom executor, Spring uses `SimpleAsyncTaskExecutor` which creates a new thread per task — no pooling, no backpressure.

### @Async and @Transactional
`@Async` methods run in a different thread, so they have no access to the caller's transaction. The `@Transactional` on `placeOrder` has already committed before `sendConfirmationAsync` runs. This is correct — you want the order saved before sending the email.

---

## 5. @Scheduled — Background Tasks

```java
// fixedDelay — waits 5 minutes AFTER the previous run finishes
@Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
@Transactional
public void detectStuckOrders() {
    Instant cutoff = Instant.now().minus(30, ChronoUnit.MINUTES);
    List<Order> stuck = orderRepository.findStuckOrders(cutoff);
    stuck.forEach(o -> o.setStatus(OrderStatus.CANCELLED));
    orderRepository.saveAll(stuck);
}

// cron — runs at 2:00 AM every day
@Scheduled(cron = "0 0 2 * * *")
public void nightlyReport() { ... }
```

`@EnableScheduling` on `OrderServiceApplication` activates scheduled task execution.

### fixedRate vs fixedDelay

| | fixedRate | fixedDelay |
|---|---|---|
| Timing | Every N ms from start of last run | N ms after end of last run |
| Overlap risk | Yes — if task takes > N ms | No — always waits N ms after completion |
| Use for | Polling at exact intervals | Tasks that must not overlap |

### Cron expression format
```
"0 0 2 * * *"
 │ │ │ │ │ └── day of week (0-7, 0=Sunday)
 │ │ │ │ └──── month (1-12)
 │ │ │ └────── day of month (1-31)
 │ │ └──────── hour (0-23)
 │ └────────── minute (0-59)
 └──────────── second (0-59)
```

Common patterns:
- `"0 */5 * * * *"` — every 5 minutes
- `"0 0 * * * *"` — every hour
- `"0 0 0 * * MON"` — every Monday at midnight

---

## 6. Saga Pattern — Distributed Transactions

### The problem
In a monolith, you can wrap everything in one `@Transactional`. In microservices, each service has its own database — there's no single transaction that spans multiple services.

**2-Phase Commit (2PC)** — the traditional solution — requires a distributed transaction coordinator. It's slow, complex, and a single point of failure. Almost no microservice system uses it.

### Saga: local transactions + compensation

A saga is a sequence of local transactions. Each step commits immediately. If a later step fails, **compensation transactions** undo the previous steps.

```
Step 1: Create order (PENDING)          ← local transaction, commits
Step 2: Reserve stock in product-service ← Feign call (simulated)
Step 3: Process payment                  ← Feign call (simulated)
Step 4: Confirm order (CONFIRMED)        ← local transaction, commits

If Step 3 fails:
  Compensate Step 2: Release stock
  Compensate Step 1: Cancel order
```

### Our implementation

```java
public OrderResponse execute(PlaceOrderRequest req) {
    // Step 1
    OrderResponse order = orderService.placeOrder(req);

    // Step 2
    try {
        reserveStock(order);
    } catch (Exception e) {
        compensateCancelOrder(order.id());   // undo step 1
        throw new RuntimeException("Stock reservation failed", e);
    }

    // Step 3
    try {
        processPayment(order);
    } catch (Exception e) {
        compensateReleaseStock(order);       // undo step 2
        compensateCancelOrder(order.id());   // undo step 1
        throw new RuntimeException("Payment failed", e);
    }

    // Step 4
    return orderService.updateStatus(order.id(), OrderStatus.CONFIRMED);
}
```

### Saga orchestration vs choreography

**Orchestration** (our approach): a central `SagaOrchestrator` coordinates all steps and compensations. Easy to understand and debug. Single point of control.

**Choreography**: each service publishes events and reacts to events from other services. No central coordinator. More resilient but harder to trace. Used with Kafka (Day 25-26).

```
Orchestration:
  SagaOrchestrator → calls each service directly

Choreography:
  OrderService publishes OrderPlaced event
  ProductService listens → reserves stock → publishes StockReserved event
  PaymentService listens → processes payment → publishes PaymentProcessed event
  OrderService listens → confirms order
```

### Idempotency in sagas
Compensation steps must be idempotent — calling them twice must have the same effect as calling once. If `cancelOrder` is called twice (e.g., due to a retry), the second call should not fail — it should just be a no-op if the order is already cancelled.

### What happens if compensation fails?
```java
private void compensateCancelOrder(Long orderId) {
    try {
        orderService.cancelOrder(orderId);
    } catch (Exception e) {
        // compensation failed — needs manual intervention
        log.error("[SAGA] COMPENSATION FAILED for order {}: {}", orderId, e.getMessage());
        // production: publish to dead letter queue, alert ops team
    }
}
```

If compensation fails, the system is in an inconsistent state. Production solutions:
- Dead letter queue — failed compensations are queued for manual review
- Outbox pattern — compensation events are persisted before being sent
- Saga state machine — track each step's status in a `saga_log` table

---

## 7. The Full Order Flow

```
POST /api/orders
    → SagaOrchestrator.execute()
        → Step 1: OrderService.placeOrder()
            → @Transactional begins
            → for each item: getProduct() [CircuitBreaker + Retry + Bulkhead]
            → check stock
            → create Order + OrderItems
            → orderRepository.save()
            → AuditLogService.logOrderCreated() [REQUIRES_NEW — own transaction]
            → sendConfirmationAsync() [fires on order-async thread, returns immediately]
            → @Transactional commits
        → Step 2: reserveStock() [validates stock via circuit-breaker-protected call]
        → Step 3: processPayment() [simulated]
        → Step 4: OrderService.updateStatus(CONFIRMED)
    → 201 Created { order details }

Meanwhile, on order-async thread:
    → sendConfirmationAsync() logs confirmation (would call notification-service in production)

Every 5 minutes, on scheduler thread:
    → detectStuckOrders() — auto-cancels PENDING orders older than 30 minutes
```

---

## 8. Key Concepts Summary

| Concept | One-line explanation |
|---|---|
| `@Transactional` | Wraps method in DB transaction — commits on success, rolls back on exception |
| `readOnly = true` | Optimization hint — skips dirty checking, enables read replica routing |
| `REQUIRED` | Join existing transaction or create new (default) |
| `REQUIRES_NEW` | Always create new transaction, suspend existing — commits independently |
| `REPEATABLE_READ` | Prevents non-repeatable reads — same row returns same data within transaction |
| `@Async` | Runs method on a separate thread — caller doesn't wait |
| `@EnableAsync` | Activates `@Async` processing (without this, `@Async` is ignored) |
| `ThreadPoolTaskExecutor` | Bounded thread pool for async tasks — prevents unbounded thread creation |
| `@Scheduled(fixedDelay)` | Runs N ms after previous execution ends |
| `@Scheduled(cron)` | Runs on a cron schedule |
| `@EnableScheduling` | Activates `@Scheduled` processing |
| Saga | Sequence of local transactions with compensation steps — replaces distributed 2PC |
| Orchestration | Central coordinator calls each saga step |
| Choreography | Services react to events — no central coordinator |
| Compensation | Undo a completed saga step when a later step fails |
| Idempotency | Calling an operation twice has the same effect as calling it once |
