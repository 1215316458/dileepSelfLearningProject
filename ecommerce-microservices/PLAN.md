# 30-Day E-Commerce Microservices Learning Plan

> **Mode**: You code, I guide. Each day has a spec — you build it, I review.
> **Time**: ~3-4 hours/day focused work
> **Project**: `C:\Users\zbxxven\SelfLearniningProject\ecommerce-microservices`

---

## Phase 1: Pure Java (Days 1–12)
No Spring. No frameworks. Just Java 17, your brain, and a `main()` method.

---

### Day 1 — BaseEntity + Enums

**What to build:**
- `BaseEntity<ID>` — abstract generic class with id, createdAt, updatedAt
- `Category` enum — with displayName, taxRate(BigDecimal), `fromString()` returning Optional
- `OrderStatus` enum — with state machine (`canTransitionTo()`, `allowedTransitions()`)
- `Role` enum — CUSTOMER, ADMIN, SELLER with displayName

**Concepts to apply:**
- Generics (type parameter `<ID>`)
- Abstraction (abstract class)
- Encapsulation (private fields, getters/setters)
- equals/hashCode (based on id)
- toString with StringBuilder (explain why not `+`)
- Enums with fields, constructors, methods
- Optional (in `Category.fromString()`)
- BigDecimal (not double for money — explain why)

**Test yourself:** Create a `main()` that:
- Tries valid and invalid OrderStatus transitions
- Parses "electronics" and "invalid" through `Category.fromString()`

---

### Day 2 — Domain Models (Product, User, CartItem, Order)

**What to build:**
- `Product` extends BaseEntity<Long> — name, description, price(BigDecimal), stock, Category
- `User` extends BaseEntity<Long> — username, email, password, Role
- `CartItem` — product reference, quantity, `getSubtotal()` method
- `Order` extends BaseEntity<Long> — userId, List<CartItem>, totalAmount, OrderStatus, shippingAddress
  - Use **Builder pattern** (inner static class) for Order
  - `transitionTo(OrderStatus)` method that throws exception on invalid transition

**Concepts to apply:**
- Inheritance (extends BaseEntity)
- Polymorphism (toString override in each class)
- Builder pattern (Order has many optional fields)
- Composition (Order HAS CartItems)
- BigDecimal arithmetic (getSubtotal = price × quantity)
- Immutability awareness (why CartItem fields should be final)

**Test yourself:** Build an Order using the Builder, add CartItems, call `transitionTo()` with valid and invalid states.

---

### Day 3 — Design Patterns + Exceptions

**What to build:**
- **Singleton**: `IdGenerator` — private constructor, static instance, `nextId()` method
- **Strategy**: `PricingStrategy` interface → `RegularPricing`, `BulkPricing`, `PremiumUserPricing`
- **Observer**: `EventPublisher` interface, `EventListener` functional interface, `EventType` enum, `InMemoryEventPublisher` impl
- **Factory**: `NotificationFactory.create(EventType)` → returns message template string
- **Exceptions**: `EcommerceException` (abstract) → `ProductNotFoundException`, `InsufficientStockException`, `InvalidOrderStateException`, `DuplicateEmailException`, `PaymentFailedException`, `UnauthorizedException`

**Concepts to apply:**
- Singleton (thread-safe? lazy vs eager?)
- Strategy (runtime algorithm swap)
- Observer (publish-subscribe, functional interface for listener)
- Factory (object creation encapsulation)
- Custom exceptions (checked vs unchecked — which and why?)
- Error codes in exceptions ("PRODUCT_001", "ORDER_002")
- Interfaces with default methods
- Functional interfaces (@FunctionalInterface)

**Test yourself:** 
- Compute price for 15 items with BulkPricing vs RegularPricing
- Publish ORDER_PLACED event, verify listeners fire
- Throw and catch ProductNotFoundException, print its errorCode

---

### Day 4 — Generic Repository + ProductRepository

**What to build:**
- `Repository<T extends BaseEntity<ID>, ID>` — generic interface (save, findById→Optional, findAll, deleteById, existsById, count)
- `InMemoryRepository<T, ID>` — abstract class implementing Repository with HashMap<ID, T>
- `ProductRepository extends InMemoryRepository<Product, Long>`:
  - `findByCategory(Category)` → List<Product>
  - `findByPriceRange(BigDecimal min, BigDecimal max)` → uses TreeMap<BigDecimal, List<Product>> as price index
  - `findByNameContaining(String)` → case-insensitive search
- Products implement `Comparable<Product>` (natural order by name)
- Static Comparators: `Product.BY_PRICE`, `Product.BY_STOCK`, `Product.BY_CREATED_DATE`

**Concepts to apply:**
- Generics with bounded type parameters (`T extends BaseEntity<ID>`)
- HashMap (O(1) lookup by id)
- TreeMap (sorted by price — O(log n) range queries)
- Comparable vs Comparator (natural order vs custom orders)
- Optional (findById returns Optional<T>)
- Collections.unmodifiableList on returned lists

**Test yourself:** Save 10 products, query by category, query by price range $50-$200, sort by different comparators.

---

### Day 5 — UserRepository + OrderRepository + Cart

**What to build:**
- `UserRepository extends InMemoryRepository<User, Long>`:
  - Internal HashSet<String> for email uniqueness
  - `findByEmail(String)` → Optional<User>
  - `findByRole(Role)` → List<User>
  - Override `save()` to check email uniqueness, throw DuplicateEmailException
- `OrderRepository extends InMemoryRepository<Order, Long>`:
  - Uses LinkedHashMap (insertion-ordered)
  - `findByUserId(Long)` → List<Order> in chronological order
  - `findByStatus(OrderStatus)` → List<Order>
  - PriorityQueue<Order> for processing queue (premium users first)
- `ShoppingCart`:
  - Map<Long, CartItem> per user
  - addItem, removeItem, updateQuantity, getItems (unmodifiable)
  - `getTotalAmount()` using PricingStrategy from Day 3

**Concepts to apply:**
- HashSet (O(1) email uniqueness check)
- LinkedHashMap (preserves insertion order)
- PriorityQueue (heap-based ordering, custom Comparator)
- Collections.unmodifiableList / Map.of / List.of
- Strategy pattern integration (cart uses pricing strategy)

**Test yourself:** 
- Try saving two users with same email → DuplicateEmailException
- Add orders, retrieve by userId in order
- Process orders from PriorityQueue — premium users come out first

---

### Day 6 — Iterator, WeakHashMap, Serialization

**What to build:**
- `RecentlyViewedTracker`:
  - LinkedHashSet<Long> per user, max 10 items
  - Implements `Iterable<Long>` with custom Iterator (reverse order — most recent first)
- `ProductCache` using WeakHashMap<Long, Product>:
  - Demonstrate entries getting GC'd when no strong references exist
- `DataPersistence` utility:
  - `serialize(Object, String filename)` — write to file
  - `deserialize(String filename)` → Object
  - Make domain objects Serializable
  - `transient` on User.password field

**Concepts to apply:**
- Custom Iterator (implement Iterator interface, hasNext/next)
- Iterable (allows for-each loop)
- LinkedHashSet (ordered, unique)
- WeakHashMap (entries eligible for GC when key has no strong references)
- Serialization / Deserialization (ObjectOutputStream, ObjectInputStream)
- transient keyword (skip fields during serialization)
- serialVersionUID (why it matters for versioning)

**Test yourself:**
- View 12 products, verify only last 10 are kept, iterate in reverse
- Cache products in WeakHashMap, null the strong references, call `System.gc()`, check cache size
- Serialize a User, deserialize it, verify password is null

---

### Day 7 — Streams: Filtering, Mapping, Collecting

**What to build:**
- `ProductFilter` — compose Predicate<Product> chains:
  - `byCategory(Category)`, `byPriceRange(min, max)`, `byInStock()`, `byNameContains(keyword)`
  - Combine: `ProductFilter.byCategory(ELECTRONICS).and(ProductFilter.byInStock())`
- `ProductTransformer` — Function<Product, ProductDTO>:
  - `toSummary` (id, name, price), `toDetailed` (all fields), `toCatalogEntry` (name, price, category)
- `ProductAnalytics` (all using Streams, ZERO loops):
  - Top N products by sales (flatMap orders→cartItems, groupingBy productId, summingInt quantity)
  - Products never ordered
  - Category distribution: groupingBy category → counting
  - Price histogram: group by price bucket ($0-50, $50-100, etc.)

**Concepts to apply:**
- Predicate<T>, Function<T,R>, Supplier<T>, Consumer<T>, BiFunction<T,U,R>
- Method references (Product::getPrice, Category::name)
- Stream pipeline: source → intermediate → terminal
- map, filter, flatMap, collect
- Collectors.groupingBy, counting, summingInt
- Stream.sorted with Comparator

**Test yourself:** Seed 20 products, 10 orders. Run each analytics method, print results.

---

### Day 8 — Streams: Advanced Analytics + Optional + Custom Collector

**What to build:**
- `OrderAnalytics` (ZERO loops):
  - Revenue per category (flatMap → groupingBy → summingDouble)
  - Monthly revenue trend (groupingBy YearMonth)
  - Average order value per user
  - High-value customers (total spend > threshold)
  - Order status distribution (partitioningBy + groupingBy)
- `InventoryAnalytics`:
  - Low stock alerts (filter + sorted)
  - Total inventory value (map to price×stock, reduce to sum)
  - Dead stock (products with zero orders in last 30 days)
- Optional chaining everywhere:
  - `findCheapestInCategory(Category)` → Optional<Product>
  - `getOrderWithHighestValue(Long userId)` → Optional<Order>
- Custom `SalesReportCollector`:
  - Implements Collector<Order, Accumulator, SalesReport>
  - Produces: totalRevenue, orderCount, averageOrderValue, topProducts

**Concepts to apply:**
- Collectors: groupingBy, partitioningBy, toMap, joining, reducing
- Optional: orElse, map, flatMap, ifPresent, orElseThrow
- Custom Collector (supplier, accumulator, combiner, finisher, characteristics)
- Nested groupingBy (Map<Category, Map<YearMonth, BigDecimal>>)
- Stream.reduce for aggregation

**Test yourself:** Generate a full sales report using your custom collector. Verify it matches manual calculation.

---

### Day 9 — Parallel Streams + Functional Composition

**What to build:**
- `BulkPriceUpdater`:
  - Update prices of 10,000 products using parallel stream
  - Measure time: sequential vs parallel
- Thread-safety demo:
  - Parallel stream writing to ArrayList (broken — show the bug)
  - Same with ConcurrentLinkedQueue (safe)
- Functional composition utilities:
  - Chain multiple Functions with `andThen`
  - Chain multiple Predicates with `and`, `or`, `negate`
  - `Supplier<Order>` factory for test data generation

**Concepts to apply:**
- Parallel streams (when they help, when they hurt)
- Thread safety with parallel streams
- Function.andThen, Function.compose
- Predicate.and, Predicate.or, Predicate.negate
- When NOT to use parallel: small collections, ordered operations, shared mutable state

**Test yourself:** 
- Time 10,000 price updates: sequential vs parallel (parallel should win)
- Time 100 price updates: sequential vs parallel (sequential should win — overhead)
- Show the ArrayList corruption with parallel stream

---

### Day 10 — Concurrency: Thread Safety + Producer-Consumer

**What to build:**
- Convert ProductRepository backing store to ConcurrentHashMap
- `InventoryManager` with ReentrantReadWriteLock:
  - readLock for getStock(), checkAvailability()
  - writeLock for reserveStock(), releaseStock()
  - Demo: 100 threads reading, 1 thread writing
- `OrderCounter` using AtomicInteger
- `RequestContext` using ThreadLocal (store userId per thread, cleanup with remove())
- `OrderQueue` — LinkedBlockingQueue producer-consumer:
  - 5 producer threads (place orders)
  - 3 consumer threads (process orders)
  - Graceful shutdown with poison pill
  - `volatile boolean running` flag

**Concepts to apply:**
- ConcurrentHashMap (thread-safe map)
- ReentrantReadWriteLock (many readers OR one writer)
- AtomicInteger (lock-free counter with CAS)
- ThreadLocal (per-thread storage, memory leak prevention)
- BlockingQueue (producer-consumer pattern)
- volatile (visibility guarantee across threads)
- wait/notify vs BlockingQueue (explain the difference)

**Test yourself:**
- Run 100 concurrent stock reads + 1 writer — no data corruption
- Run producer-consumer, verify all orders are processed exactly once
- Forget ThreadLocal.remove() — explain the memory leak

---

### Day 11 — CompletableFuture + Synchronization Primitives

**What to build:**
- `OrderProcessingService` — async pipeline:
  - validateOrder → reserveInventory → processPayment → confirmOrder → sendConfirmation
  - Each stage on a custom ExecutorService
  - `.exceptionally()` for error handling + compensation (release stock if payment fails)
  - `CompletableFuture.allOf()` — batch 50 orders in parallel
  - `CompletableFuture.anyOf()` — race multiple payment providers
- `FlashSaleManager`:
  - CountDownLatch(3): sale starts after 3 services initialize
  - Semaphore(100): max 100 concurrent buyers
  - AtomicInteger for remaining stock (compareAndSet for lock-free decrement)
  - CyclicBarrier: batch process every 10 orders
- Deadlock demo:
  - Thread 1: lock inventory → lock payment
  - Thread 2: lock payment → lock inventory
  - Fix: consistent lock ordering

**Concepts to apply:**
- CompletableFuture (thenApply, thenCompose, thenCombine, allOf, anyOf, exceptionally)
- ExecutorService (fixed thread pool, custom naming)
- CountDownLatch (one-time gate)
- CyclicBarrier (reusable synchronization point)
- Semaphore (rate limiting)
- AtomicInteger.compareAndSet (CAS — lock-free)
- Deadlock (detection and prevention via lock ordering)

**Test yourself:**
- Process 50 orders async, verify all complete (or fail gracefully)
- Run flash sale: 200 threads, 100 items — exactly 100 succeed, 100 get "sold out"
- Trigger and fix the deadlock

---

### Day 12 — Scheduled Tasks + ForkJoin + Console Checkpoint

**What to build:**
- ScheduledExecutorService tasks:
  - Every 5s: check low stock → publish STOCK_LOW event
  - Every 10s: print order metrics
  - Every 30s: clean expired carts
- ForkJoinPool: recursive inventory value calculation across category tree
- CopyOnWriteArrayList for active sessions
- ConcurrentLinkedQueue for bulk error collection
- **CHECKPOINT — `EcommerceSimulator.java`**:
  1. Seed 50 products (5 categories), 20 users
  2. Start scheduled tasks
  3. Simulate flash sale: 200 threads buying 100 limited items
  4. Run 50 concurrent orders through async pipeline
  5. Print analytics (streams)
  6. Serialize state → deserialize → verify
  7. Print: orders processed, failures, avg processing time

**Concepts to apply:**
- ScheduledExecutorService (fixedRate, fixedDelay, initialDelay)
- ForkJoinPool (RecursiveTask, work stealing)
- CopyOnWriteArrayList (frequent reads, rare writes)
- Java Memory Model awareness (stack vs heap, GC basics)
- Integration of ALL Phase 1 concepts

**Deliverable:** Running console app that proves your entire Java foundation.

---

## Phase 2: Spring Boot Services (Days 13–22)

---

### Day 13 — Product Service: Entity + Repository + Config

**What to build:**
- Spring Boot project (spring-boot-starter-web, spring-boot-starter-data-jpa, H2/MySQL)
- `Product` → JPA entity (@Entity, @Table, @Id, @GeneratedValue, @Column, @Enumerated)
- @CreatedDate, @LastModifiedDate with @EntityListeners(AuditingEntityListener)
- Database indexes (@Index on category, name)
- `ProductRepository extends JpaRepository<Product, Long>`
  - Derived queries: findByCategory, findByNameContainingIgnoreCase, findByPriceBetween
  - @Query (JPQL + native)
  - JPA Specification for dynamic filtering
  - Pagination with Pageable

**Concepts:** Auto-configuration, starters, JPA entities, JpaRepository, @Query, Specifications, Pageable, auditing, HikariCP connection pooling, DB indexing.

---

### Day 14 — Product Service: Service + Controller + Validation

**What to build:**
- `ProductService` (@Service) — CRUD + business logic
  - @Transactional(readOnly=true) on reads
  - @PostConstruct seed data in dev profile
  - @Cacheable / @CacheEvict (simple in-memory cache for now)
- `ProductController` (@RestController):
  - Full REST endpoints (GET, POST, PUT, DELETE)
  - @Valid on request bodies, @RequestParam for filtering
  - Return ResponseEntity<ApiResponse<T>> (consistent wrapper)
- Custom `@ValidCategory` annotation + validator
- Bean validation (@NotBlank, @NotNull, @Positive, @Size)

**Concepts:** @Service, @RestController, @RequestBody, @PathVariable, @RequestParam, @Valid, custom validator, ResponseEntity, @Profile, @PostConstruct, constructor injection.

---

### Day 15 — Product Service: Exception Handling + AOP + Actuator

**What to build:**
- `GlobalExceptionHandler` (@RestControllerAdvice)
  - Handle each exception type → proper HTTP status + ErrorResponse
- `LoggingAspect` (@Aspect):
  - @Around on service methods — log name, args, execution time
  - @AfterThrowing — log exceptions
- Custom `@TrackExecutionTime` annotation + aspect
- Actuator: health, metrics, info, custom HealthIndicator, custom @Endpoint

**Concepts:** @RestControllerAdvice, @ExceptionHandler, AOP (@Aspect, @Around, @Before, @AfterThrowing), custom annotations, Reflection basics, Actuator, HealthIndicator, Profiles (dev/prod yml).

---

### Day 16 — Product Service: Profiles + Testing

**What to build:**
- application.yml, application-dev.yml (H2), application-prod.yml (MySQL)
- `@Profile("dev")` data seeder
- Unit tests: `ProductServiceTest` (Mockito, @Mock, @InjectMocks)
- Controller tests: `ProductControllerTest` (@WebMvcTest, MockMvc)
- Repository tests: `ProductRepositoryTest` (@DataJpaTest)

**Concepts:** Profiles, @SpringBootTest, @WebMvcTest, @DataJpaTest, MockMvc, @MockBean, Mockito (when/thenReturn, verify).

---

### Day 17 — User Service: Entity + Security Config

**What to build:**
- New Spring Boot module: user-service
- User, Address, Role JPA entities
  - @OneToMany (User→Addresses), @ManyToMany (User↔Roles)
  - Lazy vs Eager loading, solve N+1 with @EntityGraph
- SecurityFilterChain configuration:
  - Public paths, role-based paths
  - Disable CSRF (stateless)
  - BCryptPasswordEncoder bean
- CustomUserDetailsService implements UserDetailsService

**Concepts:** @OneToMany, @ManyToOne, @ManyToMany, FetchType.LAZY, N+1 problem, @EntityGraph, SecurityFilterChain, Authentication vs Authorization, BCrypt, UserDetailsService, CSRF.

---

### Day 18 — User Service: JWT + Auth Endpoints

**What to build:**
- `JwtService`: generateToken, validateToken, extractUsername, extractClaims
  - Access token (15min), Refresh token (7 days)
  - Roles in JWT claims
- `JwtAuthenticationFilter extends OncePerRequestFilter`
- Auth endpoints: POST /register, /login, /refresh
- Profile endpoints: GET /profile, PUT /profile (authenticated)
- @PreAuthorize("hasRole('ADMIN')") on admin endpoints
- HandlerInterceptor for login rate limiting

**Concepts:** JWT, OncePerRequestFilter, @PreAuthorize, method-level security, HandlerInterceptor, Filters vs Interceptors, OAuth2 resource server concept.

---

### Day 19 — Order Service: Feign + Resilience4j

**What to build:**
- New Spring Boot module: order-service
- Order, OrderItem entities (@OneToMany cascade)
- @FeignClient(name="product-service") — call product APIs
- Resilience4j:
  - @CircuitBreaker (fallback method)
  - @Retry (exponential backoff)
  - @Bulkhead (isolate thread pool)
  - @TimeLimiter (timeout)
- Fallback: return cached data or error response

**Concepts:** OpenFeign, @FeignClient, Circuit Breaker states (closed/open/half-open), Retry with backoff, Bulkhead, TimeLimiter, fallback methods.

---

### Day 20 — Order Service: Transactions + Async + Saga

**What to build:**
- `@Transactional` on placeOrder (rollback on failure)
- `@Transactional(propagation=REQUIRES_NEW)` on audit logging
- `@Transactional(isolation=REPEATABLE_READ)` on stock check
- `@Async("orderExecutor")` for sending confirmation
- `@Scheduled` for stuck order detection, nightly archival
- Saga: sequence of Feign calls with try-catch compensation
  - Create order → reserve stock → process payment
  - If payment fails → release stock → cancel order

**Concepts:** @Transactional (propagation, isolation), @Async, TaskExecutor config, @Scheduled (fixedRate, cron), Saga orchestration vs choreography.

---

### Day 21 — Notification Service + MongoDB

**What to build:**
- New Spring Boot module: notification-service
- Notification entity stored in MongoDB (MongoRepository)
- REST endpoints to list notifications per user
- Prepare for Kafka consumption (Day 25)

**Concepts:** MongoRepository, @Document, MongoDB vs relational, eventual consistency concept.

---

### Day 22 — Integration Testing (All Services)

**What to build:**
- Integration tests for Order Service:
  - @SpringBootTest with WireMock (stub Product Service)
  - Test circuit breaker: WireMock returns 500 → verify fallback
  - TestContainers for real MySQL
- End-to-end test: register → login → create product → place order
- TestDataFactory, application-test.yml

**Concepts:** @SpringBootTest, WireMock, TestContainers, integration test vs unit test, test profiles.

---

## Phase 3: Spring Cloud Infrastructure (Days 23–24)

---

### Day 23 — Service Registry + Config Server

**What to build:**
- Service Registry (Eureka Server) on port 8761
- Config Server on port 8888 (file-based config repo)
- All services register as Eureka clients
- @RefreshScope for dynamic config reload
- Explain CAP theorem: Eureka=AP, MySQL=CA

**Concepts:** Eureka Server/Client, Config Server, @RefreshScope, centralized configuration, CAP theorem, self-preservation mode.

---

### Day 24 — API Gateway

**What to build:**
- Spring Cloud Gateway on port 8080
- Routes to all services using `lb://` (load balanced via Eureka)
- JWT Authentication Filter (validate token, pass userId downstream)
- Rate limiting with Redis (token bucket)
- Correlation ID filter (generate UUID, propagate)
- CORS configuration
- API versioning (URI-based)
- Load balancing demo: 2 instances of product-service

**Concepts:** Spring Cloud Gateway, route predicates, filters, rate limiting algorithms (token bucket, sliding window), load balancing, CORS, API versioning, correlation ID.

---

## Phase 4: Event-Driven + Hardening (Days 25–28)

---

### Day 25 — Kafka: Producers + Consumers

**What to build:**
- Kafka setup (Docker)
- Event classes: OrderPlacedEvent, StockReservedEvent, StockReservationFailedEvent, PaymentProcessedEvent
- Order Service: publish OrderPlacedEvent on order placement
- Product Service: @KafkaListener → reserve stock → publish StockReservedEvent
- Notification Service: @KafkaListener → create notification (MongoDB)
- Idempotency: track processed eventIds, skip duplicates

**Concepts:** Kafka producer/consumer, topics, partitions, consumer groups, KafkaTemplate, @KafkaListener, idempotency, at-least-once delivery.

---

### Day 26 — Saga Choreography + CQRS + Dead Letter

**What to build:**
- Full saga via Kafka events:
  - OrderPlaced → StockReserved → PaymentProcessed → OrderConfirmed
  - StockReservationFailed → OrderCancelled
- Dead letter topic: failed messages after 3 retries
- CQRS concept: write to MySQL (Order Service), read from MongoDB (Notification Service)
- Event sourcing concept (explain, don't fully implement)

**Concepts:** Saga choreography, compensation events, dead letter queue, CQRS, event sourcing, eventual consistency, backpressure (max.poll.records).

---

### Day 27 — Redis Caching + Advanced Resilience + DB Optimization

**What to build:**
- Redis caching for products (cache-aside, TTL, @CacheEvict via Kafka event)
- Redis session storage for Gateway
- Redis sorted set for "most viewed products" leaderboard
- Advanced Resilience4j: bulkhead (semaphore + thread pool), retry with jitter
- DB optimization: composite indexes, @EntityGraph, @BatchSize, HikariCP tuning
- Explain: consistent hashing, sharding, read replicas (concepts)

**Concepts:** Cache-aside, write-through, TTL, LRU eviction, Redis data structures, Bulkhead patterns, retry with jitter, DB indexing, connection pooling, sharding concept, read replicas concept.

---

### Day 28 — Observability (Tracing + Metrics + Logging)

**What to build:**
- Zipkin distributed tracing (micrometer-tracing-bridge-brave)
- Trace a request: Gateway → Order Service → Product Service → Kafka → Notification
- Correlated logging: traceId + spanId in MDC, log pattern includes them
- Custom Micrometer metrics: Counter (orders.placed), Timer (processing.time), Gauge (low_stock)
- Health aggregation at Gateway
- Explain ELK stack concept (don't implement)

**Concepts:** Zipkin, Micrometer, distributed tracing, correlation ID propagation, MDC, structured logging, Prometheus metrics, health groups (readiness/liveness), ELK concept.

---

## Phase 5: Deployment (Days 29–30)

---

### Day 29 — Docker Compose (Full Stack)

**What to build:**
- Multi-stage Dockerfile for each service
- docker-compose.yml with ALL services:
  - Infrastructure: MySQL, MongoDB, Redis, Kafka, Zookeeper, Zipkin
  - Platform: service-registry, config-server, api-gateway
  - Services: product-service, order-service, user-service, notification-service
- Health checks, depends_on with conditions
- Environment-specific configs (application-docker.yml)
- `docker-compose up` → entire system runs

**Concepts:** Multi-stage Docker builds, Docker Compose, health checks, service dependencies, environment variables, .dockerignore, volume mounts.

---

### Day 30 — Kubernetes + 12-Factor + Final Demo

**What to build:**
- Kubernetes manifests per service:
  - Deployment (replicas, resource limits, probes)
  - Service (ClusterIP, LoadBalancer for Gateway)
  - ConfigMap (non-sensitive config)
  - Secret (DB passwords, JWT secret)
- HorizontalPodAutoscaler (scale on CPU 70%)
- 12-Factor App checklist (verify each factor against our system)
- Service mesh concept explanation (Istio/Linkerd)
- **FINAL DEMO**: `docker-compose up` → run full user journey:
  - Register → Login → Browse products → Place order → Kafka event → Notification created → View in Zipkin

**Concepts:** Kubernetes (Deployment, Service, ConfigMap, Secret, HPA), liveness/readiness probes, 12-Factor App, service mesh concept, production readiness.

---

## Quick Reference: What You Can Answer After Each Phase

| After | Interview Topics You Own |
|-------|--------------------------|
| Day 12 | All core Java: OOP, collections, streams, concurrency, design patterns |
| Day 22 | Spring Boot: REST, JPA, Security, JWT, Feign, Resilience4j, Testing |
| Day 24 | Service discovery, API gateway, load balancing, rate limiting, config management |
| Day 28 | Kafka, event-driven architecture, caching, Redis, observability, CQRS |
| Day 30 | Docker, Kubernetes, system design, 12-Factor, production deployment |

---

## Rules for Yourself

1. **No copy-paste** — type every line yourself
2. **Comment your code** — explain WHY, not WHAT
3. **Test before moving on** — each day has a "test yourself" checkpoint
4. **Commit daily** — one meaningful commit per day minimum
5. **If stuck > 30 min** — ask me for a hint, not the answer
6. **If ahead of schedule** — add more test cases or edge cases, don't skip ahead

---

Ready? Ask me for **Day 1 specification** when you want to start.
