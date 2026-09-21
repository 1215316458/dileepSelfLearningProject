# Day 21 — Notification Service + MongoDB

## What We Built

A new `notification-service` (port 8084) that stores notifications in MongoDB.
It exposes REST endpoints to create, list, mark-read, and delete notifications.
It is designed to receive events from Kafka on Day 25 — today we wire the REST layer.

**Test results: 18/18 pass** (1 context load + 9 unit + 8 controller)

---

## MongoDB vs Relational — When to Choose Which

| Dimension | MongoDB (Document) | MySQL / PostgreSQL (Relational) |
|-----------|--------------------|---------------------------------|
| Schema | Flexible — fields can vary per document | Fixed — ALTER TABLE for every change |
| Joins | No joins — embed related data in document | Foreign keys + JOINs |
| Transactions | Single-document atomic by default; multi-doc needs explicit session | Full ACID across tables |
| Scaling | Horizontal sharding built-in | Vertical first, sharding complex |
| Query language | JSON-based filter syntax | SQL |
| Best for | Notifications, logs, events, catalogs, user profiles | Orders, payments, inventory (anything needing ACID) |

**Why notifications belong in MongoDB:**
- Each notification is self-contained — no need to JOIN to other collections
- Schema evolves freely (add `metadata` field without migration)
- High write volume — MongoDB handles append-heavy workloads well
- No cross-notification transactions needed

---

## Core Annotations

### `@Document`

```java
@Document(collection = "notifications")
public class Notification { ... }
```

Maps a Java class to a MongoDB collection. Equivalent to `@Entity` + `@Table` in JPA.
- `collection` = the MongoDB collection name (like a table name)
- If omitted, Spring Data uses the class name (lowercased)

### `@Id`

```java
@Id
private String id;
```

Maps to MongoDB's `_id` field. When the field type is `String`, MongoDB stores it as an `ObjectId` (24-char hex string like `507f1f77bcf86cd799439011`).

**ObjectId structure:** 4-byte timestamp + 5-byte random + 3-byte counter → globally unique, sortable by creation time.

### `@Indexed`

```java
@Indexed
private Long userId;
```

Creates a single-field index on `userId`. Without this, `findByUserId` does a full collection scan — O(n). With it, O(log n).

`auto-index-creation: true` in `application.yml` tells Spring Data to create indexes on startup.

### `@CreatedDate`

```java
@CreatedDate
private Instant createdAt;
```

Automatically populated when the document is first saved. Requires `@EnableMongoAuditing` on a `@Configuration` class.

---

## MongoRepository

```java
public interface NotificationRepository extends MongoRepository<Notification, String> {
    Page<Notification> findByUserId(Long userId, Pageable pageable);
    List<Notification> findByUserIdAndReadFalse(Long userId);
    long countByUserIdAndReadFalse(Long userId);

    @Query("{ 'userId': ?0, 'type': ?1 }")
    List<Notification> findByUserIdAndType(Long userId, NotificationType type);

    void deleteByUserId(Long userId);
}
```

`MongoRepository<T, ID>` provides the same interface contract as `JpaRepository`:
- `save()`, `findById()`, `findAll()`, `deleteById()`, `count()`, etc.
- Spring Data generates the implementation at runtime from method name conventions

**Key differences from JPA:**
- ID type is `String` (ObjectId), not `Long`
- No `@Transactional` by default — single-document operations are atomic
- No lazy loading / N+1 problem — documents are self-contained
- `@Query` uses MongoDB JSON syntax, not JPQL

### Derived Query Translation

| Method name | MongoDB filter |
|-------------|----------------|
| `findByUserId(Long id)` | `{ userId: id }` |
| `findByUserIdAndReadFalse(Long id)` | `{ userId: id, read: false }` |
| `countByUserIdAndReadFalse(Long id)` | `db.count({ userId: id, read: false })` |
| `deleteByUserId(Long id)` | `db.deleteMany({ userId: id })` |

---

## `@EnableMongoAuditing`

```java
@Configuration
@Profile("!test")
@EnableMongoAuditing
public class MongoConfig { }
```

Activates `@CreatedDate` and `@LastModifiedDate` on `@Document` classes.
Equivalent to `@EnableJpaAuditing` for MongoDB.

**Why `@Profile("!test")`?**
`@EnableMongoAuditing` registers a `mongoAuditingHandler` bean that depends on `mongoMappingContext`. In tests where `MongoDataAutoConfiguration` is excluded (no real MongoDB), `mongoMappingContext` doesn't exist → `BeanCreationException`. Putting the annotation in a `@Profile("!test")` config skips it in tests.

---

## Testing Without a Real MongoDB

Since MongoDB is not available in the offline environment, tests use two strategies:

### 1. Unit tests — `@ExtendWith(MockitoExtension.class)`

```java
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock NotificationRepository repository;
    @InjectMocks NotificationService service;
    // ...
}
```

No Spring context. No MongoDB. Pure Java. Fast.

### 2. Spring context tests — exclude Mongo autoconfiguration

`application-test.yml`:
```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
      - org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration
      - org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration
```

Then mock the repository so the service can be wired:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class NotificationControllerTest {
    @MockitoBean NotificationService notificationService;
    @MockitoBean NotificationRepository notificationRepository; // satisfies wiring
    // ...
}
```

---

## Eventual Consistency

MongoDB is designed for **eventual consistency** in distributed deployments (replica sets, sharded clusters). This means:

- A write to the primary may not be immediately visible on secondaries
- Reads from secondaries may return slightly stale data
- This is acceptable for notifications — a 100ms delay before a notification appears is fine

**Write concern** controls durability:
- `w: 1` (default) — acknowledged by primary only
- `w: majority` — acknowledged by majority of replica set members (stronger durability)

**Read concern** controls staleness:
- `local` (default) — read from primary, may not reflect latest majority-committed write
- `majority` — only return data acknowledged by majority

For notifications, the defaults are fine. For financial data, use `w: majority`.

---

## Document Design Principles

### Embed vs Reference

**Embed** (what we do):
```json
{
  "_id": "abc123",
  "userId": 1,
  "type": "ORDER_PLACED",
  "title": "Order Placed",
  "message": "Your order #100 has been placed",
  "referenceId": 100,
  "read": false,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

Each notification is complete — no need to look up anything else.

**Reference** (avoid for notifications):
```json
{ "_id": "abc123", "userId": 1, "orderId": 100 }
// Then separately: db.orders.findOne({_id: 100})
```

This requires two queries — defeats the purpose of a document database.

**Rule of thumb:**
- Embed when data is always accessed together
- Reference when data is large, changes frequently, or is shared across many documents

### Schema Evolution

MongoDB allows adding fields without migration:

```java
// v1
public record Notification(String id, Long userId, String message) {}

// v2 — just add the field, old documents still work (field will be null)
public record Notification(String id, Long userId, String message, String channel) {}
```

Old documents without `channel` return `null` for that field — no `ALTER TABLE` needed.

---

## Preparing for Kafka (Day 25)

The `NotificationService.createOrderNotification()` method is designed to be called by a Kafka consumer:

```java
// Day 25 — Kafka consumer will call this:
@KafkaListener(topics = "order-placed")
public void onOrderPlaced(OrderPlacedEvent event) {
    notificationService.createOrderNotification(
        event.userId(), event.orderId(), NotificationType.ORDER_PLACED
    );
}
```

Today we expose it via REST for testing. On Day 25, the Kafka consumer replaces the REST call.

---

## pom.xml Changes Explained

This service required the most pom.xml work because `spring-boot-starter-data-mongodb` was not in the local Maven cache at Spring Boot 4.1.1. Every change below was forced by the offline environment.

### Fix 1 — Replace the starter with direct library declarations

**Problem:** `spring-boot-starter-data-mongodb` is not cached at version 4.1.1. The SB4.1.1 BOM also points to `spring-data-mongodb 5.1.1` (via `spring-data-bom 2026.0.1`), which is also not cached.

**What was changed:**
```xml
<!-- REMOVED (not cached): -->
<!-- <artifactId>spring-boot-starter-data-mongodb</artifactId> -->

<!-- ADDED (cached at 4.2.0): -->
<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-mongodb</artifactId>
    <version>4.2.0</version>
</dependency>
```

Declaring the library directly (without the starter) bypasses the BOM version resolution entirely and uses exactly what is in the local cache.

### Fix 2 — Pin all MongoDB driver artifacts to 4.11.1

**Problem:** `spring-data-mongodb 4.2.0` has a transitive dependency on `mongodb-driver-sync`. Maven resolved this transitively and pulled `mongodb-driver-core 5.8.1` from the BOM — a version that is not cached. This caused a `ClassNotFoundException` at runtime because the 4.x and 5.x driver jars are incompatible.

**What was changed:**
```xml
<!-- ADDED — all 4 driver artifacts pinned to the cached version: -->
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-driver-sync</artifactId>
    <version>4.11.1</version>
</dependency>
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-driver-core</artifactId>
    <version>4.11.1</version>
</dependency>
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>bson</artifactId>
    <version>4.11.1</version>
</dependency>
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>bson-record-codec</artifactId>
    <version>4.11.1</version>
</dependency>
```

Pinning all four prevents Maven from upgrading any of them transitively. They must all be the same version or the driver breaks.

### Fix 3 — Add commons-logging bridge

**Problem:** `spring-data-mongodb 4.2.0` was compiled against Spring 6.x. Spring 6.x included a `commons-logging` bridge inside `spring-core` so that libraries using `org.apache.commons.logging.LogFactory` would work. Spring Boot 4.x / Spring 7.x removed this bridge. At runtime, the JVM threw `NoClassDefFoundError: org/apache/commons/logging/LogFactory` the moment any `spring-data-mongodb` class was loaded.

**What was changed:**
```xml
<!-- ADDED — provides the missing LogFactory class at runtime: -->
<dependency>
    <groupId>commons-logging</groupId>
    <artifactId>commons-logging</artifactId>
    <version>1.3.6</version>
</dependency>
```

This is a runtime-only fix. The `commons-logging` jar simply provides the class that `spring-data-mongodb 4.2.0` expects to find.

### Summary of all pom.xml changes

| Change | Why |
|--------|-----|
| Replaced `spring-boot-starter-data-mongodb` with `spring-data-mongodb 4.2.0` direct | Starter not cached at SB4.1.1; BOM points to 5.1.1 (also not cached) |
| Pinned `mongodb-driver-sync/core`, `bson`, `bson-record-codec` to `4.11.1` | BOM pulled `5.8.1` transitively; 5.x not cached and incompatible with 4.x data layer |
| Added `commons-logging 1.3.6` | Spring 7.x removed the bridge; `spring-data-mongodb 4.2.0` still needs `LogFactory` at runtime |

**In a connected environment** (corporate proxy reachable), none of these three changes would be needed. You would simply write:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```
and Spring Boot would manage all versions automatically.

---

## Spring Boot 4.x Breaking Changes (Day 21)

| Issue | Cause | Fix |
|-------|-------|-----|
| `spring-boot-starter-data-mongodb` 4.1.1 not in local cache | Offline environment, only 3.2.0 cached | Declare `spring-data-mongodb` + driver jars directly at cached versions |
| `mongodb-driver-core 5.8.1` pulled transitively | `spring-data-mongodb 4.2.0` BOM references 5.x driver | Pin `mongodb-driver-core`, `bson`, `bson-record-codec` to 4.11.1 |
| `NoClassDefFoundError: org/apache/commons/logging/LogFactory` | `spring-data-mongodb 4.2.0` built against Spring 6.x which used `commons-logging`; Spring 7.x removed it | Add `commons-logging 1.3.6` explicitly |
| `BeanCreationException: mongoAuditingHandler` | `@EnableMongoAuditing` needs `mongoMappingContext` which is excluded in tests | Move `@EnableMongoAuditing` to `@Profile("!test")` config class |
| `IllegalStateException: No primary or single unique constructor found for Pageable` | `spring-data-mongodb 4.2.0` doesn't register `PageableHandlerMethodArgumentResolver` with Spring Boot 4.x MVC | Replace `Pageable` parameter with explicit `@RequestParam page/size` |
| `Page<T>` serialization fails (500) | `PageImpl` Jackson serialization not configured with mismatched spring-data version | Return `List<T>` (call `.getContent()`) instead of `Page<T>` |

---

## Architecture: Where Notification Service Fits

```
User Service (8082)  ──────────────────────────────────────────┐
Order Service (8083) ──→ Kafka (Day 25) ──→ Notification Service (8084) ──→ MongoDB
Product Service (8081) ────────────────────────────────────────┘
```

- Order Service publishes `OrderPlacedEvent` to Kafka
- Notification Service consumes it and creates a `Notification` document in MongoDB
- User can query their notifications via REST: `GET /api/notifications/user/{userId}`

This is the **event-driven** pattern — services don't call each other directly, they communicate through events. Notification Service doesn't know about Order Service at all.
