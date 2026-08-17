## Complete Concept Map → Problem Mapping

Before the problems, here's every concept and where it fits:

### Core Java Concepts
| Concept | Problem |
|---------|---------|
| Classes, Objects, Constructors | P1 |
| Encapsulation (private fields, getters/setters) | P1 |
| Inheritance (abstract class, extends) | P1 |
| Polymorphism (method overriding, runtime dispatch) | P1 |
| Abstraction (abstract class + interface) | P1 |
| Interfaces, default methods, functional interfaces | P1 |
| Enums with behavior | P1 |
| Builder pattern, Factory pattern | P1 |
| 🆕 Singleton pattern | P1 |
| 🆕 Strategy pattern | P1 |
| 🆕 Observer pattern | P1 |
| equals, hashCode, toString | P1 |
| Custom exceptions, checked vs unchecked | P1 |
| 🆕 Generics (generic classes, methods, bounded types) | P2 |
| 🆕 Comparable vs Comparator | P2 |
| 🆕 Iterable, Iterator, custom iterator | P2 |
| HashMap, LinkedHashMap, TreeMap | P2 |
| HashSet, LinkedHashSet, TreeSet | P2 |
| ArrayList, LinkedList | P2 |
| PriorityQueue, ArrayDeque | P2 |
| 🆕 Collections.unmodifiableList, List.of, Map.of | P2 |
| 🆕 WeakHashMap (cache eviction concept) | P2 |
| Stream pipeline (source → intermediate → terminal) | P3 |
| map, filter, reduce, collect, flatMap | P3 |
| groupingBy, partitioningBy, toMap, joining | P3 |
| 🆕 Optional (orElse, map, flatMap, ifPresent) | P3 |
| 🆕 Parallel streams + when NOT to use | P3 |
| 🆕 Custom Collector | P3 |
| 🆕 Method references (Class::method) | P3 |
| 🆕 Predicate, Function, Supplier, Consumer, BiFunction | P3 |
| Thread, Runnable, Callable | P4 |
| synchronized, volatile | P4 |
| ReentrantLock, ReadWriteLock | P4 |
| 🆕 wait/notify/notifyAll | P4 |
| ExecutorService, ScheduledExecutorService | P4 |
| CompletableFuture (thenApply, thenCompose, allOf) | P4 |
| ConcurrentHashMap, CopyOnWriteArrayList | P4 |
| CountDownLatch, CyclicBarrier, Semaphore | P4 |
| 🆕 AtomicInteger, AtomicReference, CAS | P4 |
| 🆕 ThreadLocal | P4 |
| 🆕 BlockingQueue (producer-consumer) | P4 |
| 🆕 ForkJoinPool | P4 |
| 🆕 Deadlock detection and prevention | P4 |
| 🆕 String, StringBuilder, StringPool, immutability | P1 |
| 🆕 Serialization / Deserialization | P2 |
| 🆕 Reflection basics | P5 |
| 🆕 Annotations (custom annotations) | P5 |
| 🆕 Java Memory Model (stack vs heap, GC basics) | P4 |

### Spring Boot Concepts
| Concept | Problem |
|---------|---------|
| Auto-configuration, starters | P5 |
| Dependency Injection (constructor injection) | P5 |
| 🆕 @Component, @Service, @Repository, @Controller | P5 |
| 🆕 Bean lifecycle, @PostConstruct, @PreDestroy | P5 |
| 🆕 @Qualifier, @Primary, @Conditional | P5 |
| 🆕 Profiles (@Profile, application-dev.yml) | P5 |
| REST controllers, @RequestBody, @PathVariable | P5 |
| 🆕 @RequestParam, @RequestHeader, @ModelAttribute | P5 |
| Bean validation (@NotBlank, @Valid, custom validator) | P5 |
| Global exception handling (@RestControllerAdvice) | P5 |
| 🆕 ResponseEntity, custom response wrapper | P5 |
| 🆕 Interceptors (HandlerInterceptor) | P6 |
| 🆕 Filters (OncePerRequestFilter) | P6 |
| 🆕 AOP (@Aspect, @Around, @Before) for logging | P6 |
| 🆕 @Async for async processing | P7 |
| 🆕 @Scheduled for cron jobs | P7 |
| Actuator (health, metrics, info) | P5 |
| 🆕 Custom Actuator endpoint | P5 |
| 🆕 Testing: @SpringBootTest, @WebMvcTest, MockMvc | P8 |
| 🆕 Testing: @MockBean, @DataJpaTest | P8 |

### Spring Data Concepts
| Concept | Problem |
|---------|---------|
| JpaRepository, CrudRepository | P5 |
| 🆕 Query methods (findByNameContaining, etc.) | P5 |
| 🆕 @Query (JPQL + native) | P5 |
| 🆕 JPA Specifications (dynamic queries) | P5 |
| Pagination and Sorting (Pageable) | P5 |
| 🆕 Entity relationships (@OneToMany, @ManyToOne, @ManyToMany) | P6 |
| 🆕 Lazy vs Eager loading, N+1 problem | P6 |
| 🆕 @Transactional, propagation, isolation levels | P7 |
| 🆕 Auditing (@CreatedDate, @LastModifiedDate) | P5 |
| 🆕 MongoDB repositories (MongoRepository) | P7 |
| 🆕 Redis template operations | P9 |

### Spring Security Concepts
| Concept | Problem |
|---------|---------|
| 🆕 SecurityFilterChain configuration | P6 |
| 🆕 Authentication vs Authorization | P6 |
| BCrypt password encoding | P6 |
| JWT generation and validation | P6 |
| 🆕 UserDetailsService, custom UserDetails | P6 |
| Role-based access (@PreAuthorize) | P6 |
| 🆕 Method-level security | P6 |
| CORS configuration | P10 |
| 🆕 CSRF protection (when to enable/disable) | P6 |
| 🆕 OAuth2 basics (resource server) | P6 |

### Spring Cloud / Microservices Concepts
| Concept | Problem |
|---------|---------|
| Service Discovery (Eureka) | P9 |
| Config Server (centralized config) | P9 |
| API Gateway (routing, filters) | P10 |
| 🆕 Load Balancing (Spring Cloud LoadBalancer) | P10 |
| OpenFeign (declarative HTTP client) | P7 |
| Circuit Breaker (Resilience4j) | P7 |
| 🆕 Retry, Rate Limiter, Bulkhead, TimeLimiter | P12 |
| 🆕 Saga pattern (orchestration vs choreography) | P11 |
| Kafka producer/consumer | P11 |
| 🆕 Event sourcing concepts | P11 |
| 🆕 Idempotency handling | P11 |
| 🆕 Dead letter queue | P11 |
| Distributed tracing (Zipkin) | P13 |
| 🆕 Correlation ID propagation | P13 |
| 🆕 Centralized logging (ELK concept) | P13 |
| 🆕 Service mesh concepts | P14 |
| 🆕 API versioning | P10 |

### System Design Concepts
| Concept | Problem |
|---------|---------|
| 🆕 CAP theorem | P9 |
| 🆕 Consistent hashing | P12 |
| Caching strategies (cache-aside, write-through) | P12 |
| 🆕 Cache eviction policies (LRU, TTL) | P12 |
| 🆕 Database sharding concepts | P12 |
| 🆕 Database replication (read replicas) | P12 |
| 🆕 Rate limiting algorithms (token bucket, sliding window) | P10 |
| 🆕 Idempotency keys | P11 |
| 🆕 Eventual consistency | P11 |
| 🆕 CQRS (Command Query Responsibility Segregation) | P11 |
| 🆕 Backpressure handling | P11 |
| 🆕 Connection pooling (HikariCP) | P5 |
| 🆕 Database indexing strategies | P5 |
| 🆕 API design best practices (REST maturity model) | P5 |
| 🆕 Pagination strategies (offset vs cursor) | P5 |
| 🆕 12-Factor App principles | P14 |

---

## Revised Problem Statements (All Concepts Linked)

### Problem 1: Domain Foundation (Core Java — OOP, Design Patterns, Strings, Exceptions)

**Concepts**: Classes, Objects, Encapsulation, Inheritance, Polymorphism, Abstraction, Interfaces, Enums, Builder, Singleton, Strategy, Observer, Generics intro, String immutability, Custom exceptions

Build the complete domain layer:

**1a. Base Infrastructure**
- Abstract `BaseEntity<ID>` using generics — fields: id (generic type), createdAt, updatedAt (LocalDateTime)
- Override `equals`/`hashCode` based on id, `toString` using StringBuilder (not string concatenation — explain why: String immutability and StringPool)
- `IdGenerator` as a Singleton (private constructor, static instance) that generates sequential IDs

**1b. Domain Models**
- `Product` extends BaseEntity<Long> — name, description, price (BigDecimal, not double — explain precision), stock, Category enum
- `Category` enum with fields: displayName, taxRate. Methods: `getTaxRate()`, `fromString(String)` that returns Optional<Category>
- `User` extends BaseEntity<Long> — username, email, password, Role enum (CUSTOMER, ADMIN, SELLER)
- `CartItem` — product reference, quantity, `getSubtotal()` method
- `Order` — userId, List<CartItem>, totalAmount, OrderStatus, shippingAddress
  - Use Builder pattern (inner static class) since Order has many optional fields
  - `OrderStatus` enum: PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED
  - `OrderStatus.canTransitionTo(OrderStatus next)` — enforces valid state machine transitions
  - `Order.transitionTo(OrderStatus)` throws `InvalidOrderStateException` if invalid

**1c. Design Patterns**
- Strategy pattern: `PricingStrategy` interface with `calculatePrice(Product, int quantity)`
  - Implementations: `RegularPricing`, `BulkPricing` (>10 items = 10% off), `PremiumUserPricing` (5% off always)
  - `PricingContext` holds a strategy and delegates
- Observer pattern: `EventPublisher` interface with `publish(Event)`, `subscribe(EventType, EventListener)`
  - `EventListener` functional interface with `onEvent(Event)`
  - `EventType` enum: ORDER_PLACED, ORDER_SHIPPED, STOCK_LOW, USER_REGISTERED
  - `InMemoryEventPublisher` implementation using Map<EventType, List<EventListener>>
  - This exact pattern becomes Kafka events in Problem 11
- Factory pattern: `NotificationFactory.create(EventType)` returns appropriate notification message template

**1d. Exception Hierarchy**
- `EcommerceException` (abstract, RuntimeException) with errorCode (String) and message
- Subclasses: `ProductNotFoundException`, `InsufficientStockException`, `InvalidOrderStateException`, `DuplicateEmailException`, `PaymentFailedException`, `UnauthorizedException`
- Each carries a specific error code like "PRODUCT_001", "ORDER_002"

**Output**: `domain/` package — models, enums, patterns, exceptions. Every class is reused in Phase 2.

---

### Problem 2: Data Layer (Collections, Generics, Comparable, Comparator, Serialization, Iterator)

**Concepts**: HashMap, LinkedHashMap, TreeMap, HashSet, TreeSet, ArrayList, LinkedList, PriorityQueue, Generics, Comparable, Comparator, Iterator, unmodifiable collections, WeakHashMap, Serialization

Build the storage and query layer using Problem 1's domain:

**2a. Generic Repository Pattern**
- `Repository<T extends BaseEntity<ID>, ID>` generic interface: save, findById (returns Optional<T>), findAll, deleteById, existsById, count
- `InMemoryRepository<T, ID>` abstract class implementing it with HashMap<ID, T>
- This teaches the exact same pattern Spring Data JPA uses

**2b. Concrete Repositories**
- `ProductRepository extends InMemoryRepository<Product, Long>`
  - `findByCategory(Category)` → returns List<Product>
  - `findByPriceRange(BigDecimal min, BigDecimal max)` → uses internal TreeMap<BigDecimal, List<Product>> as price index
  - `findByNameContaining(String keyword)` → case-insensitive search
  - Products implement `Comparable<Product>` (natural order by name)
  - Provide Comparators: `Product.BY_PRICE`, `Product.BY_STOCK`, `Product.BY_CREATED_DATE`
- `UserRepository extends InMemoryRepository<User, Long>`
  - Internal HashSet<String> for email uniqueness check
  - `findByEmail(String)` → returns Optional<User>
  - `findByRole(Role)` → returns List<User>
- `OrderRepository extends InMemoryRepository<Order, Long>`
  - Uses LinkedHashMap (insertion-ordered) as backing store
  - `findByUserId(Long)` → returns List<Order> in chronological order
  - `findByStatus(OrderStatus)` → returns List<Order>
  - PriorityQueue<Order> for order processing queue — premium users first (custom Comparator)

**2c. Cart & Browsing**
- `ShoppingCart` — Map<Long, CartItem> per user
  - addItem, removeItem, updateQuantity, getItems (returns unmodifiable list via `Collections.unmodifiableList`)
  - `getTotalAmount()` using pricing strategy from Problem 1
- `RecentlyViewedTracker` — LinkedHashSet<Long> per user, max 10 items
  - Custom iterator that traverses in reverse (most recent first)
  - Implement `Iterable<Long>` with custom `Iterator`
- `ProductCache` using WeakHashMap<Long, Product> — demonstrate how entries get GC'd when no strong references exist (concept: cache eviction)

**2d. Serialization**
- Make all domain objects implement `Serializable`
- `DataPersistence` utility: serialize entire repository state to a file, deserialize back
- Demonstrate `transient` keyword on password field in User
- This concept maps to how objects are sent over network in microservices (JSON serialization)

**Output**: `repository/` package. Same interface names and method signatures that Spring Data auto-implements in Problem 5.

**Link to Problem 1**: Every repository stores and queries Problem 1's domain objects. PricingStrategy is used in Cart. Observer events fire on save/delete.

---

### Problem 3: Business Logic (Streams, Optional, Functional Interfaces, Method References)

**Concepts**: Stream pipeline, map, filter, reduce, flatMap, collect, groupingBy, partitioningBy, toMap, joining, Optional, Predicate, Function, Supplier, Consumer, BiFunction, method references, parallel streams, custom Collector

Build analytics and processing on top of Problem 2's repositories:

**3a. Functional Utilities**
- `ProductFilter` — compose Predicate<Product> chains:
  - `byCategory(Category)`, `byPriceRange(min, max)`, `byInStock()`, `byNameContains(keyword)`
  - Combine: `ProductFilter.byCategory(ELECTRONICS).and(ProductFilter.byPriceRange(100, 500))`
- `ProductTransformer` — Function<Product, ProductDTO> for different views:
  - `toSummary` (id, name, price only), `toDetailed` (all fields), `toCatalogEntry` (name, price, category)
- `Supplier<Order>` — `OrderFactory` that creates sample orders for testing
- `Consumer<Order>` — `OrderLogger` that logs order details
- `BiFunction<Product, Integer, BigDecimal>` — price calculator with quantity

**3b. Stream-Based Analytics (all using Streams, zero loops)**
- `ProductAnalytics`
  - Top N products by sales: flatMap orders → cartItems, groupingBy productId, summingInt quantity, sort, limit
  - Products never ordered: filter products not present in any order
  - Category distribution: groupingBy category, counting — returns Map<Category, Long>
  - Price histogram: groupingBy price bucket ($0-50, $50-100, etc.) using custom classifier
- `OrderAnalytics`
  - Revenue per category: flatMap → groupingBy category → summingDouble
  - Monthly revenue trend: groupingBy YearMonth → summingDouble — returns Map<YearMonth, BigDecimal>
  - Average order value per user: groupingBy userId → averagingDouble
  - Full report: Map<Category, Map<YearMonth, BigDecimal>> using nested groupingBy
  - High-value customers: filter users with total spend > threshold
  - Order status distribution: partitioningBy (completed vs not), then groupingBy status
- `InventoryAnalytics`
  - Low stock alerts: filter stock < threshold, sorted ascending
  - Total inventory value: map to (price × stock), reduce to sum
  - Dead stock: products with zero orders in last 30 days
  - Restock suggestions: products where (orders per week × 4) > current stock

**3c. Optional Usage**
- All findById methods return Optional<T>
- Chain: `findById(id).map(Product::getPrice).filter(p -> p.compareTo(limit) < 0).orElseThrow(() -> new ProductNotFoundException(id))`
- `findCheapestInCategory(Category)` → returns Optional<Product> using Stream.min
- `getOrderWithHighestValue(Long userId)` → Optional<Order>

**3d. Custom Collector**
- Build `SalesReportCollector` that collects a stream of Orders into a `SalesReport` object containing: totalRevenue, orderCount, averageOrderValue, topProducts
- Implement `Collector<Order, SalesReportAccumulator, SalesReport>` with supplier, accumulator, combiner, finisher

**3e. Parallel Streams**
- `BulkPriceUpdater`: update prices of 10,000 products using parallel stream
- Demonstrate when parallel is slower (small collections, ordered operations)
- Show thread-safety issue: parallel stream writing to ArrayList (broken) vs ConcurrentLinkedQueue (safe)
- This leads directly into Problem 4's concurrency deep dive

**Output**: `service/` and `analytics/` packages. These method signatures become Spring @Service methods in Problem 5-7.

**Link to Problem 2**: All analytics read from Problem 2's repositories. Functional utilities transform Problem 1's domain objects.

---

### Problem 4: Concurrency (Threads, Locks, Executors, CompletableFuture, Concurrent Collections, JMM)

**Concepts**: Thread, Runnable, Callable, synchronized, volatile, ReentrantLock, ReadWriteLock, wait/notify, ExecutorService, ScheduledExecutorService, CompletableFuture, ConcurrentHashMap, CopyOnWriteArrayList, BlockingQueue, CountDownLatch, CyclicBarrier, Semaphore, AtomicInteger, AtomicReference, ThreadLocal, ForkJoinPool, deadlock, Java Memory Model

Make the system from Problems 1-3 handle concurrent users:

**4a. Thread-Safe Repositories**
- Convert `ProductRepository` backing store to ConcurrentHashMap
- `InventoryManager` with ReentrantReadWriteLock:
  - `readLock` for `getStock()`, `checkAvailability()` (many concurrent readers)
  - `writeLock` for `reserveStock()`, `releaseStock()` (exclusive writer)
  - Demonstrate: 100 threads reading stock concurrently while 1 thread updates
- `OrderCounter` using AtomicInteger — track total orders processed without locks
- `RequestContext` using ThreadLocal — store current userId per thread (like Spring's SecurityContextHolder)
  - Remember to clean up with `remove()` to prevent memory leaks

**4b. Producer-Consumer: Order Pipeline**
- `OrderQueue` using LinkedBlockingQueue<Order>
  - Producer threads: simulate users placing orders, put into queue
  - Consumer threads: order processors that take from queue
  - Demonstrate: 5 producers, 3 consumers, graceful shutdown with poison pill pattern
- `volatile boolean running` flag to signal shutdown across threads
- Show difference: without volatile (threads may not see update) vs with volatile

**4c. Async Order Processing with CompletableFuture**
- `OrderProcessingService` — process an order through stages:
  ```
  validateOrder(order)                          // Stage 1
    .thenCompose(valid -> reserveInventory(order))  // Stage 2
    .thenCompose(reserved -> processPayment(order)) // Stage 3
    .thenCompose(paid -> confirmOrder(order))        // Stage 4
    .thenApply(confirmed -> sendConfirmation(order)) // Stage 5
    .exceptionally(ex -> handleFailure(order, ex))   // Error handling
  ```
  - Each stage runs on a custom ExecutorService
  - If payment fails → compensate by releasing reserved inventory
  - `CompletableFuture.allOf()` — process batch of 50 orders in parallel, wait for all
  - `CompletableFuture.anyOf()` — query multiple payment providers, use first response
  - This exact pattern becomes inter-service async calls in Problem 7

**4d. Synchronization Primitives**
- `FlashSaleManager`:
  - `CountDownLatch(3)`: sale starts only when InventoryService, PricingService, and NotificationService are all initialized
  - `Semaphore(100)`: max 100 concurrent purchase attempts
  - `AtomicInteger` for remaining flash sale stock — use `compareAndSet` for lock-free decrement
  - `CyclicBarrier`: batch processing — wait for 10 orders to accumulate, then process batch together
- Demonstrate deadlock:
  - Thread 1 locks inventory then tries to lock payment
  - Thread 2 locks payment then tries to lock inventory
  - Show detection and fix (consistent lock ordering)

**4e. Scheduled Tasks**
- `ScheduledExecutorService`:
  - Every 5 seconds: check for low stock and publish STOCK_LOW event (using Observer from Problem 1)
  - Every 10 seconds: print order processing metrics
  - Every 30 seconds: clean up expired cart sessions
  - This becomes Spring's @Scheduled in Problem 7

**4f. Concurrent Collections in Action**
- `ConcurrentHashMap` for live product view counts (multiple threads incrementing)
- `CopyOnWriteArrayList` for active user sessions (frequent reads, rare writes)
- `ConcurrentLinkedQueue` for error collection during bulk processing
- `ForkJoinPool` for recursive inventory calculation across category tree

**4g. Java Memory Model Awareness**
- Demonstrate visibility issue without synchronization
- Show happens-before relationships
- Stack vs Heap: explain where objects, references, and primitives live
- GC basics: why short-lived objects in streams are efficient (young generation)

**Output**: `concurrent/` package. CompletableFuture patterns → async Feign calls. BlockingQueue → Kafka. Scheduled tasks → @Scheduled. ThreadLocal → SecurityContext.

**Link to Problem 3**: Uses same services and analytics, now concurrent. Parallel stream issues from 3e are solved properly here.

---

### Checkpoint: Console Application
Create `EcommerceSimulator.java`:
1. Seed 50 products across 5 categories, 20 users
2. Start scheduled tasks (stock monitor, metrics)
3. Simulate flash sale: 200 threads trying to buy 100 limited items
4. Run 50 concurrent orders through the async pipeline
5. Print full analytics report using streams
6. Serialize state to file, deserialize, verify
7. Print: orders processed, failures, average processing time

**This proves your entire Java foundation works before adding Spring.**

---

### Problem 5: Product Service — Spring Boot (REST, JPA, Validation, Caching, Actuator, AOP)

**Concepts**: Auto-configuration, DI, @Component/@Service/@Repository, Bean lifecycle, @PostConstruct, @Qualifier, @Primary, Profiles, REST controllers, @RequestParam, @RequestHeader, Bean validation, custom validator, @RestControllerAdvice, ResponseEntity, JpaRepository, @Query, JPA Specifications, Pagination, Auditing, Actuator, custom endpoint, AOP, HikariCP, DB indexing, API design, cursor pagination, Reflection basics, Custom annotations

Convert Problem 1-3's product logic into a Spring Boot microservice:

**5a. Entity Layer**
- `Product` → JPA entity with `@Entity`, `@Table`, `@Id`, `@GeneratedValue`
- `@Column` constraints: name (unique, not null), price (precision=10, scale=2)
- `@Enumerated(EnumType.STRING)` for Category
- `@CreatedDate`, `@LastModifiedDate` with `@EntityListeners(AuditingEntityListener.class)`
- Database indexes: `@Index` on category, on name for search performance
- Explain HikariCP connection pooling (auto-configured by Spring Boot)

**5b. Repository Layer**
- `ProductRepository extends JpaRepository<Product, Long>`
- Derived queries: `findByCategory`, `findByNameContainingIgnoreCase`, `findByPriceBetween`
- `@Query("SELECT p FROM Product p WHERE p.stock < :threshold")` — JPQL
- `@Query(value = "SELECT * FROM products WHERE ...", nativeQuery = true)` — native
- JPA Specification: `ProductSpecification` for dynamic multi-field filtering
- Pagination: return `Page<Product>` with `Pageable` parameter
- Cursor-based pagination: `findByIdGreaterThanOrderByIdAsc(Long cursor, Pageable page)`

**5c. Service Layer**
- `ProductService` with `@Service` — CRUD + business logic from Problem 3
- `@Cacheable("products")` on findById, `@CacheEvict` on update/delete
- `@Transactional(readOnly = true)` on read methods
- `@PostConstruct` — seed initial data in dev profile
- Inject `PricingStrategy` using `@Qualifier` — demonstrate `@Primary` for default

**5d. Controller Layer**
- REST endpoints following REST maturity model:
  - `GET /api/products` — list with pagination, filtering via @RequestParam
  - `GET /api/products/{id}` — single product
  - `POST /api/products` — create (ADMIN only, validated with @Valid)
  - `PUT /api/products/{id}` — update
  - `DELETE /api/products/{id}` — soft delete
  - `GET /api/products/search?keyword=&category=&minPrice=&maxPrice=` — dynamic search
- `@RequestHeader("X-Correlation-Id")` — accept tracing header
- Return `ResponseEntity<ApiResponse<T>>` with consistent wrapper: `{success, data, error, timestamp}`
- Custom validator: `@ValidCategory` annotation that checks if category string is valid enum value
  - This teaches custom annotations + reflection (how Spring processes annotations)

**5e. Exception Handling**
- `@RestControllerAdvice GlobalExceptionHandler`
  - Handle `ProductNotFoundException` → 404
  - Handle `MethodArgumentNotValidException` → 400 with field errors
  - Handle `DataIntegrityViolationException` → 409
  - Handle generic `Exception` → 500
  - Return consistent `ErrorResponse` with errorCode, message, timestamp, path

**5f. AOP — Cross-Cutting Concerns**
- `@Aspect LoggingAspect`:
  - `@Around` on all service methods — log method name, args, execution time
  - `@AfterThrowing` — log exceptions with stack trace
- `@Aspect PerformanceAspect`:
  - Custom annotation `@TrackExecutionTime`
  - `@Around` advice that measures and logs slow methods (>500ms)
- This teaches how Spring internally works (proxies, reflection)

**5g. Actuator & Monitoring**
- Enable health, metrics, info, prometheus endpoints
- Custom `HealthIndicator`: check DB connectivity + Redis connectivity
- Custom `@Endpoint("inventory")` — expose inventory summary via actuator
- Configure `info` endpoint with build info

**5h. Profiles & Configuration**
- `application.yml` — common config
- `application-dev.yml` — H2 database, debug logging
- `application-prod.yml` — MySQL, connection pool tuning
- `@Profile("dev")` on data seeder bean

**Output**: Complete Product Service on port 8081.

**Link to Problem 1-3**: Domain models become JPA entities. InMemoryRepository becomes JpaRepository. Stream analytics become @Service methods. PricingStrategy is injected via DI. Custom exceptions are handled by @RestControllerAdvice.

---

### Problem 6: User Service — Spring Security (Authentication, Authorization, JWT, Relationships)

**Concepts**: SecurityFilterChain, Authentication vs Authorization, BCrypt, JWT, UserDetailsService, @PreAuthorize, method security, CSRF, OAuth2 basics, @OneToMany, @ManyToOne, @ManyToMany, Lazy vs Eager, N+1 problem, Filters, Interceptors

**6a. Entity Layer**
- `User` → JPA entity with `@Entity`
- `Address` entity with `@ManyToOne` to User (User has `@OneToMany` addresses)
- `Role` entity with `@ManyToMany` — User can have multiple roles
- Demonstrate Lazy loading: `@OneToMany(fetch = FetchType.LAZY)`
- Show N+1 problem: fetching 10 users triggers 10 address queries → fix with `@EntityGraph` or `JOIN FETCH`

**6b. Security Configuration**
- `SecurityConfig` with `SecurityFilterChain` bean:
  - `/api/auth/**` — permitAll
  - `/api/admin/**` — hasRole("ADMIN")
  - `/api/users/**` — authenticated
  - Disable CSRF (stateless API) — explain when CSRF matters (browser sessions)
- `CustomUserDetailsService implements UserDetailsService` — load user from DB
- `BCryptPasswordEncoder` bean for password hashing

**6c. JWT Implementation**
- `JwtService`: generateToken, validateToken, extractUsername, extractClaims
  - Access token (15 min expiry), Refresh token (7 days)
  - Store roles in JWT claims
- `JwtAuthenticationFilter extends OncePerRequestFilter`:
  - Extract token from Authorization header
  - Validate and set SecurityContext
  - This filter is reused in API Gateway (Problem 10)

**6d. Auth Endpoints**
- `POST /api/auth/register` — register with validation, encode password
- `POST /api/auth/login` — authenticate, return JWT tokens
- `POST /api/auth/refresh` — refresh access token
- `GET /api/users/profile` — get current user (from SecurityContext)
- `PUT /api/users/profile` — update profile
- `POST /api/users/addresses` — add address
- `@PreAuthorize("hasRole('ADMIN')")` on admin endpoints
- Rate limiting on login: `HandlerInterceptor` that tracks attempts per IP using ConcurrentHashMap with TTL

**6e. OAuth2 Concept**
- Configure as OAuth2 Resource Server (validate JWT)
- Explain: how this would integrate with an external identity

---

complete the response

---




Continuing from Problem 6e...

---

**6e. OAuth2 Concept (continued)**
- Configure as OAuth2 Resource Server (validate JWT)
- Explain: how this would integrate with an external identity provider (Keycloak, Auth0)
- `spring.security.oauth2.resourceserver.jwt.issuer-uri` — concept of token issuer validation
- This is conceptual — the JWT implementation from 6c is the practical version

**Output**: Complete User Service on port 8083.

**Link to Problem 1**: User domain model, Role enum, DuplicateEmailException. Email uniqueness from Problem 2's HashSet becomes a DB unique constraint. ThreadLocal from Problem 4 maps to SecurityContextHolder.

---

### Problem 7: Order Service — Inter-Service Communication, Resilience, Transactions, Async

**Concepts**: OpenFeign, @FeignClient, RestTemplate vs WebClient vs Feign, Circuit Breaker, Retry, Bulkhead, TimeLimiter, @Transactional propagation & isolation, @Async, @Scheduled, MongoRepository, Saga pattern concept

**7a. Entity Layer**
- `Order` → JPA entity, `OrderItem` → JPA entity
- `@OneToMany(cascade = CascadeType.ALL)` Order → OrderItems
- `@Enumerated(EnumType.STRING)` for OrderStatus
- State machine validation from Problem 1 in `@PreUpdate` lifecycle callback

**7b. Feign Client — Call Product Service**
- `@FeignClient(name = "product-service")`
  - `getProduct(Long id)` → GET /api/products/{id}
  - `reduceStock(Long id, int quantity)` → PUT /api/products/{id}/stock
- Feign error decoder: map 404 → ProductNotFoundException, 400 → BadRequestException
- Compare: RestTemplate (imperative, old) vs WebClient (reactive) vs Feign (declarative) — explain why Feign fits here

**7c. Resilience4j**
- `@CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")`
  - Closed → Open after 5 failures in 10 seconds
  - Open → Half-Open after 30 seconds
  - Half-Open → Closed after 3 successful calls
- `@Retry(name = "productService", maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))`
- `@Bulkhead(name = "productService", maxConcurrentCalls = 10)` — isolate thread pool so Product Service failures don't exhaust Order Service threads
- `@TimeLimiter(name = "productService", timeoutDuration = 3s)`
- Fallback: return cached product data from local Redis cache
- Configuration in `application.yml`:
  ```yaml
  resilience4j:
    circuitbreaker:
      instances:
        productService:
          slidingWindowSize: 10
          failureRateThreshold: 50
          waitDurationInOpenState: 30s
  ```

**7d. Transaction Management**
- `@Transactional` on placeOrder:
  - Create order → call Product Service to reduce stock → save order
  - If Product Service call fails → transaction rolls back
- `@Transactional(propagation = Propagation.REQUIRES_NEW)` on audit logging — audit log persists even if main transaction fails
- `@Transactional(isolation = Isolation.REPEATABLE_READ)` on stock check — prevent dirty reads during concurrent orders
- Demonstrate: what happens without @Transactional (partial writes)

**7e. Async Processing**
- `@Async("orderExecutor")` on sendOrderConfirmation — don't block the response
- Custom `TaskExecutor` bean with pool size configuration
- `@Scheduled(fixedRate = 60000)` — check for stuck orders (PROCESSING for > 10 min) and alert
- `@Scheduled(cron = "0 0 2 * * ?")` — nightly job to archive old completed orders
- Link to Problem 4: @Async uses the same ExecutorService concept, @Scheduled replaces ScheduledExecutorService

**7f. Saga Pattern (Conceptual + Simplified Implementation)**
- Place Order Saga (choreography style):
  ```
  1. Order Service: Create order (PENDING)
  2. Product Service: Reserve stock
  3. Payment Service: Process payment (simulated)
  4. Order Service: Confirm order
  
  Compensation (if step 3 fails):
  2'. Product Service: Release stock
  1'. Order Service: Cancel order
  ```
- Implement as a sequence of Feign calls with try-catch compensation
- Explain: choreography (events, what we'll build in Problem 11) vs orchestration (central coordinator)
- This is the distributed version of Problem 4's CompletableFuture pipeline

**Output**: Complete Order Service on port 8082.

**Link to Problem 4**: CompletableFuture chain → Feign + CircuitBreaker. Compensation logic → Saga. ScheduledExecutorService → @Scheduled. The synchronous pipeline is now distributed.

---

### Problem 8: Testing (Unit, Integration, E2E)

**Concepts**: @SpringBootTest, @WebMvcTest, MockMvc, @MockBean, @DataJpaTest, @TestContainers, Mockito, AssertJ, test slices, test profiles

**8a. Unit Tests — Product Service**
- `ProductServiceTest` with `@ExtendWith(MockitoExtension.class)`
  - Mock `ProductRepository` with `@Mock`
  - Inject into `ProductService` with `@InjectMocks`
  - Test: findById returns product, findById throws exception, create validates input
  - Verify: `verify(repository, times(1)).save(any())`

**8b. Controller Tests**
- `ProductControllerTest` with `@WebMvcTest(ProductController.class)`
  - `@MockBean ProductService`
  - Use `MockMvc` to test:
    - GET /api/products → 200 with JSON array
    - GET /api/products/999 → 404 with error response
    - POST /api/products with invalid body → 400 with validation errors
    - POST /api/products without auth → 401
  - Verify response structure with `jsonPath("$.success").value(true)`

**8c. Repository Tests**
- `ProductRepositoryTest` with `@DataJpaTest`
  - Auto-configures embedded H2
  - Test: save and findById, findByCategory returns correct list, pagination works
  - Test custom @Query methods

**8d. Integration Tests**
- `OrderServiceIntegrationTest` with `@SpringBootTest`
  - Use `@TestContainers` with MySQL container
  - WireMock to stub Product Service responses
  - Test full flow: place order → verify DB state → verify Feign call made
  - Test circuit breaker: WireMock returns 500 → verify fallback triggered

**8e. Test Utilities**
- `TestDataFactory` — builders for test entities (reuse Problem 1's Builder pattern)
- `@TestConfiguration` for test-specific beans
- `application-test.yml` profile

**Output**: `src/test/` in each service.

**Link to all previous problems**: Tests validate every layer built in Problems 5-7. MockMvc tests the REST layer. @DataJpaTest tests the JPA layer. Integration tests verify Feign + Resilience4j.

---

### Problem 9: Service Registry + Config Server (Service Discovery, Centralized Config, CAP Theorem)

**Concepts**: Eureka Server/Client, Config Server, @RefreshScope, profiles, CAP theorem, service registration, heartbeat, self-preservation

**9a. Service Registry (Eureka Server — Port 8761)**
- New Spring Boot app with `@EnableEurekaServer`
- Configure self-preservation mode (explain: why Eureka doesn't deregister services during network partition — AP system in CAP)
- Dashboard at http://localhost:8761

**9b. All Services Register as Eureka Clients**
- Add `spring-cloud-starter-netflix-eureka-client` to Product, Order, User, Notification services
- `eureka.client.serviceUrl.defaultZone=http://localhost:8761/eureka/`
- `eureka.instance.preferIpAddress=true`
- Service IDs: `product-service`, `order-service`, `user-service`, `notification-service`
- Demonstrate: start 2 instances of Product Service on different ports → both appear in Eureka
- Order Service's Feign client now resolves `product-service` via Eureka instead of hardcoded URL

**9c. Config Server (Port 8888)**
- New Spring Boot app with `@EnableConfigServer`
- Config source: local file system (or Git repo)
- Structure:
  ```
  config-repo/
    application.yml          # shared config (Redis, Kafka, JWT secret)
    product-service.yml      # product-specific
    product-service-dev.yml  # product dev profile
    product-service-prod.yml # product prod profile
    order-service.yml
    user-service.yml
    notification-service.yml
  ```
- All services add `spring.config.import=optional:configserver:http://localhost:8888`
- `@RefreshScope` on beans that read config values → POST /actuator/refresh to reload without restart
- Encrypt sensitive values (DB passwords, JWT secret) using Config Server encryption

**9d. CAP Theorem Discussion**
- Eureka = AP (Available + Partition tolerant, eventually consistent)
- MySQL = CA (Consistent + Available, single node)
- Explain: why microservices favor AP for service discovery
- MongoDB (used in Notification Service) = CP or AP depending on config

**Output**: Service Registry + Config Server running. All services discover each other dynamically.

**Link to Problem 7**: Feign clients now use Eureka service names. No more hardcoded URLs. Config values externalized from application.yml.

---

### Problem 10: API Gateway (Routing, Security, Rate Limiting, Load Balancing, API Versioning)

**Concepts**: Spring Cloud Gateway, route predicates, filters, JWT validation at gateway, rate limiting algorithms (token bucket, sliding window), Redis-based rate limiter, CORS, load balancing, API versioning, correlation ID

**10a. Gateway Setup (Port 8080)**
- Spring Cloud Gateway with routes:
  ```yaml
  routes:
    - id: product-service
      uri: lb://product-service
      predicates:
        - Path=/api/products/**
    - id: order-service
      uri: lb://order-service
      predicates:
        - Path=/api/orders/**
    - id: user-service
      uri: lb://user-service
      predicates:
        - Path=/api/users/**, /api/auth/**
    - id: notification-service
      uri: lb://notification-service
      predicates:
        - Path=/api/notifications/**
  ```
- `lb://` prefix enables load balancing via Eureka

**10b. JWT Authentication Filter**
- `JwtAuthenticationFilter extends AbstractGatewayFilterFactory`
  - Extract JWT from Authorization header
  - Validate using the same JwtService logic from Problem 6
  - Add userId and roles to request headers (downstream services read these)
  - Skip filter for public paths: /api/auth/register, /api/auth/login
  - Return 401 for invalid/expired tokens

**10c. Rate Limiting**
- `RequestRateLimiterGatewayFilterFactory` with Redis
- Token bucket algorithm: 10 requests per second per user
- Explain algorithms:
  - Token bucket (smooth traffic)
  - Sliding window (precise counting)
  - Fixed window (simple but bursty)
- Custom `KeyResolver`: rate limit by userId (from JWT) for authenticated, by IP for anonymous
- Return 429 Too Many Requests with Retry-After header

**10d. Cross-Cutting Filters**
- `CorrelationIdFilter`: generate UUID if X-Correlation-Id header missing, pass to downstream
  - This becomes the traceId in Problem 13
- `LoggingFilter`: log method, path, response status, latency for every request
- `CORS configuration`: allow specific origins, methods, headers

**10e. Load Balancing**
- Start 2 instances of Product Service (ports 8081, 8091)
- Gateway round-robins between them via Eureka
- Demonstrate: kill one instance → Eureka deregisters → Gateway routes to surviving instance

**10f. API Versioning**
- Strategy: URI versioning `/api/v1/products`, `/api/v2/products`
- Route both to product-service, different controller methods handle versions
- Explain alternatives: header versioning, query param versioning — pros/cons of each

**Output**: API Gateway on port 8080. Single entry point for all services.

**Link to Problem 6**: JWT validation logic reused from User Service. SecurityContext concept from Problem 4's ThreadLocal.

---

### Problem 11: Event-Driven Architecture (Kafka, Saga, CQRS, Idempotency, Eventual Consistency)

**Concepts**: Kafka producer/consumer, topics, partitions, consumer groups, event sourcing concepts, Saga choreography, CQRS, idempotency, dead letter topic, eventual consistency, backpressure

**11a. Kafka Setup & Event Definitions**
- Topics: `order-events`, `stock-events`, `notification-events`, `dead-letter`
- Event classes (shared library or duplicated per service):
  ```java
  OrderPlacedEvent { orderId, userId, items[], totalAmount, timestamp, eventId(UUID) }
  OrderShippedEvent { orderId, userId, trackingNumber, timestamp, eventId }
  StockReservedEvent { orderId, productId, quantity, eventId }
  StockReservationFailedEvent { orderId, productId, reason, eventId }
  PaymentProcessedEvent { orderId, amount, status, eventId }
  ```
- Every event has a unique `eventId` for idempotency

**11b. Order Service — Producer**
- When order is placed:
  1. Save order with status PENDING
  2. Publish `OrderPlacedEvent` to `order-events` topic
  3. Return order to client immediately (async processing)
- When order is shipped: publish `OrderShippedEvent`
- Use `KafkaTemplate.send()` with callback for delivery confirmation
- Transactional outbox pattern concept: save event to DB + publish (ensure at-least-once delivery)

**11c. Product Service — Consumer**
- `@KafkaListener(topics = "order-events", groupId = "product-service")`
- On `OrderPlacedEvent`:
  - Reserve stock for each item
  - If successful → publish `StockReservedEvent`
  - If insufficient stock → publish `StockReservationFailedEvent`
- Idempotency: store processed eventIds in a `Set<UUID>` (DB table in production)
  - If eventId already processed → skip (handles Kafka redelivery)

**11d. Notification Service — Consumer**
- `@KafkaListener(topics = "order-events", groupId = "notification-service")`
- On `OrderPlacedEvent` → create "Order confirmed" notification
- On `OrderShippedEvent` → create "Order shipped" notification
- Store in MongoDB via MongoRepository

**11e. Saga Choreography (Distributed Transaction)**
```
Order Service                Product Service              Notification Service
     |                            |                            |
     |-- OrderPlacedEvent ------->|                            |
     |                            |-- StockReservedEvent ----->|
     |<-- StockReservedEvent -----|                            |
     |   (update order: CONFIRMED)|                            |
     |                            |                            |
     |-- OrderShippedEvent ------>|                            |
     |                            |                            |-- notify user
     
Compensation (if stock reservation fails):
     |<-- StockReservationFailed -|
     |   (update order: CANCELLED)|
     |-- OrderCancelledEvent ---->|                            |-- notify user
```
- Each service reacts to events and publishes new events
- No central coordinator — services are loosely coupled
- Compare with Problem 7f's orchestration approach

**11f. Dead Letter Topic**
- If consumer throws exception 3 times → message goes to `dead-letter` topic
- `DeadLetterConsumer` logs failed events for manual investigation
- Configure: `spring.kafka.consumer.properties.max.poll.records=10` for backpressure

**11g. CQRS Concept**
- Write model: Order Service writes to MySQL (normalized)
- Read model: Notification Service stores denormalized view in MongoDB (userId + all their notifications)
- Explain: separate read/write models for different query patterns
- Eventual consistency: read model may lag behind write model by milliseconds

**11h. Event Sourcing Concept**
- Instead of storing current state, store all events:
  - OrderCreated → ItemAdded → ItemAdded → PaymentProcessed → OrderConfirmed
  - Replay events to reconstruct current state
- Explain: when to use (audit-heavy domains like banking) vs when not to (simple CRUD)
- We don't fully implement this — just explain the concept and how it relates to our Kafka events

**Output**: All services communicating asynchronously via Kafka.

**Link to Problem 1**: Observer pattern from Problem 1c is now Kafka. EventType enum maps to Kafka topics. EventListener becomes @KafkaListener. The in-memory EventPublisher is replaced by a distributed message broker.

**Link to Problem 4**: BlockingQueue from Problem 4b is conceptually what Kafka provides at scale. Producer-consumer pattern is now distributed.

---

### Problem 12: Caching, Resilience & Performance (Redis, Cache Patterns, Resilience Patterns, DB Optimization)

**Concepts**: Cache-aside, write-through, cache invalidation, TTL, LRU eviction, Redis data structures, consistent hashing concept, database sharding concept, read replicas, connection pooling, Resilience4j dashboard, bulkhead patterns

**12a. Redis Caching Strategy**
- Product catalog cache:
  - `@Cacheable(value = "products", key = "#id")` — cache-aside pattern
  - TTL: 10 minutes via `RedisCacheConfiguration`
  - `@CacheEvict` on product update/delete
- Cache invalidation via Kafka:
  - Product Service publishes `ProductUpdatedEvent` when product changes
  - All instances of Product Service consume → evict local cache
  - Solves: multiple instances having stale cache
- Redis data structures:
  - `String` — cached product JSON
  - `Hash` — user session data
  - `Sorted Set` — product leaderboard (most viewed)
  - `List` — recent orders queue
- Explain LRU eviction: when Redis memory is full, least recently used keys are removed
- Explain consistent hashing: how Redis Cluster distributes keys across nodes

**12b. Session Management**
- Store user sessions in Redis (shared across Gateway instances)
- `RedisTemplate<String, UserSession>` operations
- Session TTL: 30 minutes, refresh on activity
- This enables horizontal scaling of Gateway — any instance can validate session

**12c. Advanced Resilience Patterns**
- Resilience4j dashboard configuration (expose via actuator)
- Bulkhead patterns:
  - Semaphore bulkhead: limit concurrent calls (like Problem 4's Semaphore)
  - Thread pool bulkhead: isolate thread pools per downstream service
  - If Product Service thread pool is exhausted, Order Service can still call User Service
- Retry with jitter: `exponentialRandomBackoff` to prevent thundering herd
- Fallback chain: cache → default value → error response
- Configuration:
  ```yaml
  resilience4j:
    bulkhead:
      instances:
        productService:
          maxConcurrentCalls: 25
        userService:
          maxConcurrentCalls: 15
    ratelimiter:
      instances:
        productService:
          limitForPeriod: 50
          limitRefreshPeriod: 1s
  ```

**12d. Database Optimization**
- Indexing strategy:
  - Single column: product name, category
  - Composite: (category, price) for filtered range queries
  - Explain: B-tree index, when indexes hurt (write-heavy tables)
- Query optimization:
  - `@EntityGraph` to solve N+1 (from Problem 6)
  - `@BatchSize` for batch loading
  - Explain EXPLAIN ANALYZE for query plans
- Connection pooling (HikariCP):
  ```yaml
  spring.datasource.hikari:
    maximum-pool-size: 20
    minimum-idle: 5
    connection-timeout: 30000
    idle-timeout: 600000
  ```
- Sharding concept: split orders table by userId range (shard 1: users 1-1000, shard 2: 1001-2000)
- Read replicas concept: write to primary MySQL, read from replica — explain replication lag

**Output**: Production-grade caching, resilience, and performance optimization across all services.

**Link to Problem 2**: WeakHashMap cache from Problem 2c is now Redis. TreeMap price index is now a DB index. Problem 4's Semaphore is now Resilience4j Bulkhead.

---

### Problem 13: Observability (Distributed Tracing, Logging, Metrics)

**Concepts**: Zipkin, Micrometer, Prometheus, correlation ID, MDC, structured logging, custom metrics, health aggregation, centralized logging (ELK concept)

**13a. Distributed Tracing with Zipkin**
- Add `micrometer-tracing-bridge-brave` + `zipkin-reporter-brave` to all services
- Configuration:
  ```yaml
  management.tracing.sampling.probability: 1.0
  management.zipkin.tracing.endpoint: http://localhost:9411/api/v2/spans
  ```
- Trace a full request:
  ```
  Client → Gateway (span 1) → Order Service (span 2) → Product Service (span 3)
                                     ↓
                              Kafka → Notification Service (span 4)
  ```
- View in Zipkin UI: see latency per service, identify bottlenecks
- Custom span: wrap payment processing in a manual span for granular timing

**13b. Correlated Logging**
- `spring.application.name` appears in every log line
- TraceId and SpanId automatically added to MDC by Micrometer
- Log format: `%d{yyyy-MM-dd HH:mm:ss} [%X{traceId}] [%X{spanId}] %-5level %logger{36} - %msg%n`
- Correlation ID from Gateway (Problem 10d) propagates as traceId
- Explain ELK stack concept:
  - Elasticsearch: store logs
  - Logstash: collect and parse logs from all services
  - Kibana: search and visualize logs
  - We don't implement ELK but explain how it fits

**13c. Custom Metrics with Micrometer**
- `MeterRegistry` injection in services:
  ```java
  Counter ordersPlaced = registry.counter("orders.placed", "status", "success");
  Timer orderProcessingTime = registry.timer("orders.processing.time");
  Gauge.builder("inventory.low_stock", this, svc -> svc.getLowStockCount())
       .register(registry);
  ```
- Metrics exposed at `/actuator/prometheus`
- Key metrics:
  - `orders.placed` (counter) — total orders by status
  - `orders.processing.time` (timer) — p50, p95, p99 latency
  - `inventory.low_stock` (gauge) — current low stock product count
  - `http.server.requests` (auto) — request count, latency per endpoint
- Explain: Prometheus scrapes /actuator/prometheus → Grafana visualizes

**13d. Health Aggregation**
- Each service: `/actuator/health` shows DB, Redis, Kafka, Eureka health
- Gateway aggregates: call all services' health endpoints → return combined status
- Custom health groups:
  ```yaml
  management.endpoint.health.group:
    readiness:
      include: db, redis
    liveness:
      include: ping
  ```

**Output**: Full observability — trace any request across all services, monitor metrics, aggregate health.

**Link to Problem 10**: Correlation ID from Gateway becomes traceId. Link to Problem 4: ThreadLocal concept maps to MDC (stores traceId per thread).

---

### Problem 14: Deployment & Production Readiness (Docker, Kubernetes, 12-Factor)

**Concepts**: Multi-stage Docker builds, Docker Compose, Kubernetes Deployments, Services, ConfigMaps, Secrets, HPA, probes, 12-Factor App, service mesh concept

**14a. Dockerize Each Service**
- Multi-stage Dockerfile:
  ```dockerfile
  # Stage 1: Build
  FROM maven:3.9-eclipse-temurin-17 AS build
  WORKDIR /app
  COPY pom.xml .
  RUN mvn dependency:go-offline        # Cache dependencies
  COPY src ./src
  RUN mvn package -DskipTests
  
  # Stage 2: Runtime
  FROM eclipse-temurin:17-jre-alpine
  COPY --from=build /app/target/*.jar app.jar
  EXPOSE 8081
  ENTRYPOINT ["java", "-jar", "app.jar"]
  ```
- Explain: why multi-stage (smaller image, no build tools in production)
- `.dockerignore` to exclude target/, .git/, etc.

**14b. Docker Compose — Full Stack**
```yaml
services:
  mysql:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping"]
  
  mongodb:
    image: mongo:7
  
  redis:
    image: redis:7-alpine
  
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on: [zookeeper]
  
  zipkin:
    image: openzipkin/zipkin
  
  service-registry:
    build: ./service-registry
    ports: ["8761:8761"]
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
  
  config-server:
    build: ./config-server
    depends_on:
      service-registry:
        condition: service_healthy
  
  api-gateway:
    build: ./api-gateway
    ports: ["8080:8080"]
    depends_on: [service-registry, config-server]
  
  product-service:
    build: ./product-service
    depends_on: [mysql, redis, kafka, service-registry, config-server]
    deploy:
      replicas: 2    # 2 instances for load balancing demo
  
  order-service:
    build: ./order-service
    depends_on: [mysql, kafka, service-registry, config-server]
  
  user-service:
    build: ./user-service
    depends_on: [mysql, redis, service-registry, config-server]
  
  notification-service:
    build: ./notification-service
    depends_on: [mongodb, kafka, service-registry, config-server]
```
- Startup order: infra → registry → config → gateway → services
- Health checks ensure dependencies are ready before dependents start

**14c. Kubernetes Manifests**
- Per service:
  - `Deployment` with readiness/liveness probes
  - `Service` (ClusterIP for internal, LoadBalancer for Gateway)
  - `ConfigMap` for non-sensitive config
  - `Secret` for DB passwords, JWT secret
- Example Product Service:
  ```yaml
  apiVersion: apps/v1
  kind: Deployment
  metadata:
    name: product-service
  spec:
    replicas: 2
    template:
      spec:
        containers:
          - name: product-service
            image: product-service:latest
            ports:
              - containerPort: 8081
            readinessProbe:
              httpGet:
                path: /actuator/health/readiness
                port: 8081
              initialDelaySeconds: 30
              periodSeconds: 10
            livenessProbe:
              httpGet:
                path: /actuator/health/liveness
                port: 8081
              initialDelaySeconds: 60
              periodSeconds: 30
            resources:
              requests:
                memory: "256Mi"
                cpu: "250m"
              limits:
                memory: "512Mi"
                cpu: "500m"
            envFrom:
              - configMapRef:
                  name: product-service-config
              - secretRef:
                  name: db-credentials
  ```

**14d. Horizontal Pod Autoscaler**
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: product-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: product-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

**14e. 12-Factor App Checklist**
Map each factor to what we've built:

| Factor | Implementation |
|--------|---------------|
| 1. Codebase | One repo per service (or monorepo with modules) |
| 2. Dependencies | Maven pom.xml explicitly declares all |
| 3. Config | Config Server + environment variables |
| 4. Backing services | MySQL, MongoDB, Redis, Kafka as attached resources |
| 5. Build, release, run | Multi-stage Docker build → image → container |
| 6. Processes | Stateless services, state in DB/Redis |
| 7. Port binding | Each service self-contained with embedded Tomcat |
| 8. Concurrency | Scale via Kubernetes replicas |
| 9. Disposability | Fast startup, graceful shutdown |
| 10. Dev/prod parity | Same Docker images, different configs |
| 11. Logs | Stdout → collected by platform |
| 12. Admin processes | @Scheduled jobs, actuator endpoints |

**14f. Service Mesh Concept**
- Explain: Istio/Linkerd sits between services as sidecar proxies
- Handles: mTLS, traffic management, observability without code changes
- Compare: what we built in code (circuit breaker, retry, tracing) vs what a service mesh provides at infrastructure level
- We don't implement — just explain where it fits

**Output**: Fully containerized and orchestrated system.

**Link to everything**: Packages the entire system from Problems 1-13 for production.

---

## Complete Chain Visualization

```
PHASE 1: PURE JAVA
═══════════════════
P1: Domain Models (OOP, Patterns, Enums, Exceptions, Strings)
 │   Product, User, Order, CartItem, OrderStatus
 │   Builder, Singleton, Strategy, Observer, Factory
 │
 ▼
P2: In-Memory Repositories (Collections, Generics, Serialization)
 │   HashMap, TreeMap, LinkedHashSet, PriorityQueue
 │   Generic Repository<T,ID>, Comparable, Comparator, Iterator
 │
 ▼
P3: Business Logic (Streams, Optional, Functional Interfaces)
 │   Analytics, Search, Filtering
 │   groupingBy, flatMap, custom Collector, parallel streams
 │
 ▼
P4: Concurrency (Threads, Locks, Executors, CompletableFuture)
 │   Thread-safe repos, async pipeline, flash sale
 │   BlockingQueue, Semaphore, AtomicInteger, ThreadLocal
 │
 ▼
 ╔═══════════════════════════════════════════════╗
 ║  CHECKPOINT: Console App — proves everything  ║
 ╚═══════════════════════════════════════════════╝
 │
 ▼
PHASE 2: SPRING BOOT MICROSERVICES
══════════════════════════════════
P5: Product Service (REST, JPA, Validation, Cache, AOP, Actuator)
 │   Domain → JPA entities, HashMap → JpaRepository
 │   Stream analytics → @Service, PricingStrategy → DI
 │
P6: User Service (Security, JWT, Relationships, Filters)
 │   User model → JPA + Security, Role enum → @PreAuthorize
 │   ThreadLocal → SecurityContextHolder
 │
P7: Order Service (

---

continue

---




Continuing from the chain visualization...

---

```
P7: Order Service (Feign, Resilience4j, Transactions, Async, Saga)
 │   CompletableFuture chain → Feign + CircuitBreaker
 │   Compensation logic → Saga pattern
 │   ScheduledExecutorService → @Scheduled
 │
P8: Testing (Unit, Integration, E2E)
 │   Validates every layer from P5-P7
 │   MockMvc, @DataJpaTest, WireMock, TestContainers
 │
 ▼
PHASE 3: SPRING CLOUD INFRASTRUCTURE
═════════════════════════════════════
P9: Service Registry + Config Server (Eureka, Config, CAP)
 │   Hardcoded URLs → Eureka discovery
 │   application.yml → Config Server
 │   CAP theorem applied to our tech choices
 │
P10: API Gateway (Routing, JWT Filter, Rate Limiting, Load Balancing)
 │    Single entry point, JWT from P6 validated here
 │    Rate limiting algorithms, correlation ID generation
 │    API versioning strategy
 │
 ▼
PHASE 4: EVENT-DRIVEN + HARDENING
═════════════════════════════════
P11: Kafka Events (Saga Choreography, CQRS, Idempotency)
 │    Observer pattern (P1) → Kafka topics
 │    BlockingQueue (P4) → distributed messaging
 │    Sync Feign calls → async event-driven
 │    Eventual consistency, dead letter queue
 │
P12: Caching + Resilience + DB Optimization
 │    WeakHashMap (P2) → Redis
 │    Semaphore (P4) → Bulkhead
 │    TreeMap index (P2) → DB index
 │    Connection pooling, sharding concepts
 │
P13: Observability (Zipkin, Metrics, Logging)
 │    Correlation ID (P10) → distributed trace
 │    ThreadLocal (P4) → MDC
 │    Custom metrics, health aggregation
 │    ELK concept for centralized logging
 │
 ▼
PHASE 5: DEPLOYMENT
═══════════════════
P14: Docker + Kubernetes + 12-Factor
      Containerize everything from P1-P13
      Docker Compose for local, K8s for production
      HPA, probes, ConfigMaps, Secrets
      Service mesh concept
```

---

## Concept → Problem Quick Reference (Alphabetical)

| Concept | Problem | How It's Used |
|---------|---------|---------------|
| Abstract classes | P1 | BaseEntity<ID> |
| AOP (@Aspect) | P5 | Logging, performance tracking |
| API Gateway | P10 | Spring Cloud Gateway routing |
| API versioning | P10 | URI-based versioning strategy |
| @Async | P7 | Async order confirmation |
| AtomicInteger/Reference | P4 | Flash sale stock counter |
| Auditing (@CreatedDate) | P5 | Auto-set timestamps on entities |
| Authentication vs Authorization | P6 | JWT login vs role checks |
| Auto-configuration | P5 | Spring Boot magic explained |
| Backpressure | P11 | Kafka consumer poll config |
| BCrypt | P6 | Password hashing |
| Bean lifecycle | P5 | @PostConstruct data seeder |
| BigDecimal vs double | P1 | Price precision |
| BlockingQueue | P4 | Order processing pipeline |
| Builder pattern | P1 | Order construction |
| Bulkhead | P12 | Thread pool isolation per service |
| Cache-aside pattern | P12 | Redis product cache |
| Cache eviction (LRU, TTL) | P12 | Redis memory management |
| CAP theorem | P9 | Eureka=AP, MySQL=CA |
| Circuit breaker | P7 | Resilience4j on Feign calls |
| Collections.unmodifiable | P2 | Immutable cart item list |
| Comparable vs Comparator | P2 | Product sorting strategies |
| CompletableFuture | P4 | Async order pipeline |
| ConcurrentHashMap | P4 | Thread-safe product store |
| ConfigMap/Secret (K8s) | P14 | Externalized config in K8s |
| Config Server | P9 | Centralized configuration |
| Connection pooling (HikariCP) | P12 | DB connection management |
| Consistent hashing | P12 | Redis Cluster key distribution |
| Constructor injection | P5 | Spring DI best practice |
| CopyOnWriteArrayList | P4 | Active user sessions |
| Correlation ID | P10 | Request tracing header |
| CORS | P10 | Gateway CORS config |
| CountDownLatch | P4 | Flash sale readiness gate |
| CQRS | P11 | Separate read/write models |
| CSRF | P6 | Disabled for stateless API |
| Cursor pagination | P5 | Efficient large dataset paging |
| Custom annotations | P5 | @ValidCategory, @TrackExecutionTime |
| Custom Collector | P3 | SalesReportCollector |
| CyclicBarrier | P4 | Batch order processing |
| Database indexing | P12 | B-tree, composite indexes |
| Database sharding | P12 | Conceptual: split by userId |
| @DataJpaTest | P8 | Repository layer testing |
| Dead letter queue | P11 | Failed Kafka messages |
| Deadlock | P4 | Detection and prevention |
| Dependency Injection | P5 | Constructor injection in services |
| Docker Compose | P14 | Full stack orchestration |
| Docker multi-stage | P14 | Optimized container images |
| ELK concept | P13 | Centralized logging architecture |
| Encapsulation | P1 | Private fields, getters/setters |
| @EntityGraph | P6 | Solve N+1 problem |
| Enums with behavior | P1 | OrderStatus.canTransitionTo() |
| Eureka | P9 | Service discovery |
| Event sourcing concept | P11 | Event replay for state |
| Eventual consistency | P11 | Read model lags write model |
| Exception hierarchy | P1 | Custom RuntimeExceptions |
| ExecutorService | P4 | Thread pool management |
| Factory pattern | P1 | NotificationFactory |
| Feign client | P7 | Declarative HTTP calls |
| Filters (OncePerRequestFilter) | P6 | JWT authentication filter |
| flatMap | P3 | Order → CartItems expansion |
| ForkJoinPool | P4 | Recursive inventory calc |
| Functional interfaces | P3 | Predicate, Function, Consumer |
| GC basics | P4 | Young generation, stream objects |
| Generics | P2 | Repository<T extends BaseEntity, ID> |
| Global exception handler | P5 | @RestControllerAdvice |
| groupingBy | P3 | Revenue per category |
| HandlerInterceptor | P6 | Login rate limiting |
| HashMap internals | P2 | Product storage |
| Health checks | P5 | Custom HealthIndicator |
| HPA (K8s) | P14 | Auto-scale on CPU |
| Idempotency | P11 | Deduplicate Kafka events |
| Immutability (String) | P1 | Why StringBuilder for concat |
| Inheritance | P1 | BaseEntity → Product |
| Interfaces | P1 | PricingStrategy, Discountable |
| Iterator (custom) | P2 | RecentlyViewedTracker |
| Java Memory Model | P4 | Visibility, happens-before |
| JPA Specifications | P5 | Dynamic product search |
| JPQL + native queries | P5 | Custom repository queries |
| JWT | P6 | Token generation/validation |
| Kafka | P11 | Event streaming |
| Kubernetes | P14 | Production orchestration |
| Lazy vs Eager loading | P6 | @OneToMany fetch strategy |
| LinkedHashMap | P2 | Insertion-ordered orders |
| LinkedHashSet | P2 | Recently viewed products |
| Load balancing | P10 | lb:// prefix in Gateway |
| Liveness/Readiness probes | P14 | K8s health checks |
| MDC (Mapped Diagnostic Context) | P13 | TraceId in logs |
| Method references | P3 | Product::getPrice |
| Method-level security | P6 | @PreAuthorize |
| Metrics (Micrometer) | P13 | Counter, Timer, Gauge |
| Mockito / @MockBean | P8 | Unit test mocking |
| MockMvc | P8 | Controller testing |
| MongoDB | P7 | Notification storage |
| MongoRepository | P7 | Notification CRUD |
| N+1 problem | P6 | Lazy loading pitfall |
| OAuth2 concept | P6 | Resource server concept |
| Observer pattern | P1 | EventPublisher → Kafka |
| @OneToMany, @ManyToMany | P6 | User-Address, User-Role |
| Optional | P3 | Safe null handling |
| Outbox pattern concept | P11 | Reliable event publishing |
| Pagination (Pageable) | P5 | Product listing |
| Parallel streams | P3 | Bulk price update |
| partitioningBy | P3 | Completed vs pending orders |
| Polymorphism | P1 | PricingStrategy dispatch |
| @PreAuthorize | P6 | Role-based method security |
| PriorityQueue | P2 | Premium order processing |
| Profiles (@Profile) | P5 | dev/prod configuration |
| Prometheus | P13 | Metrics scraping |
| @Qualifier / @Primary | P5 | Multiple bean selection |
| Rate limiting | P10 | Token bucket algorithm |
| Read replicas concept | P12 | MySQL replication |
| Reflection basics | P5 | How Spring processes annotations |
| @RefreshScope | P9 | Dynamic config reload |
| @RequestParam, @RequestHeader | P5 | Query params, headers |
| Resilience4j | P7, P12 | Circuit breaker, retry, bulkhead |
| @RestControllerAdvice | P5 | Global error handling |
| ResponseEntity | P5 | HTTP response control |
| Retry with backoff | P7 | Transient failure handling |
| Saga pattern | P7, P11 | Distributed transactions |
| @Scheduled | P7 | Cron jobs, periodic tasks |
| ScheduledExecutorService | P4 | Stock monitor, metrics |
| SecurityFilterChain | P6 | Spring Security config |
| Semaphore | P4 | Concurrent purchase limit |
| Serialization | P2 | Object persistence |
| Service mesh concept | P14 | Istio/Linkerd overview |
| Singleton pattern | P1 | IdGenerator |
| Sliding window | P10 | Rate limiting algorithm |
| @SpringBootTest | P8 | Integration testing |
| Stack vs Heap | P4 | Memory model |
| Strategy pattern | P1 | PricingStrategy |
| StringBuilder | P1 | Efficient string building |
| StringPool | P1 | String immutability |
| synchronized | P4 | Basic thread safety |
| @TestContainers | P8 | Real DB in tests |
| ThreadLocal | P4 | Per-thread request context |
| TimeLimiter | P7 | Timeout on service calls |
| @Transactional | P7 | Propagation, isolation |
| transient keyword | P2 | Skip password serialization |
| TreeMap | P2 | Price range index |
| TreeSet | P2 | Sorted unique collections |
| 12-Factor App | P14 | Production checklist |
| UserDetailsService | P6 | Custom user loading |
| volatile | P4 | Visibility guarantee |
| wait/notify | P4 | Thread communication |
| WeakHashMap | P2 | GC-friendly cache |
| @WebMvcTest | P8 | Controller slice test |
| WireMock | P8 | Stub external services |
| Zipkin | P13 | Distributed tracing UI |

---

## Timeline

| Week | Problem | What You Build | Key Concepts Practiced |
|------|---------|---------------|----------------------|
| 1 | P1 | Domain models, patterns, exceptions | OOP, Design Patterns, Enums, Generics intro |
| 2 | P2 | In-memory data layer | Collections, Generics, Comparable, Iterator, Serialization |
| 3 | P3 | Analytics & search engine | Streams, Optional, Functional Interfaces, Custom Collector |
| 4-5 | P4 | Concurrent order system | Threads, Locks, CompletableFuture, Concurrent Collections |
| 5 | Checkpoint | Console app simulation | Integration of P1-P4 |
| 6 | P5 | Product Service | Spring Boot, JPA, REST, Validation, AOP, Caching |
| 7 | P6 | User Service | Spring Security, JWT, Relationships, Filters |
| 8 | P7 | Order Service | Feign, Resilience4j, Transactions, Saga |
| 9 | P8 | Test suite | Unit, Integration, E2E testing |
| 10 | P9 | Registry + Config | Eureka, Config Server, CAP theorem |
| 10 | P10 | API Gateway | Routing, JWT filter, Rate limiting, Load balancing |
| 11 | P11 | Kafka events | Event-driven, Saga choreography, CQRS, Idempotency |
| 12 | P12 | Caching + Resilience | Redis strategies, Bulkhead, DB optimization |
| 12 | P13 | Observability | Zipkin, Metrics, Correlated logging |
| 13 | P14 | Deployment | Docker, Kubernetes, 12-Factor |

---

## Interview Readiness After Each Phase

| After Phase | You Can Answer |
|-------------|---------------|
| P1-P4 | All core Java, collections, streams, concurrency interview questions |
| P5-P8 | Spring Boot, REST API design, JPA, Security, Testing questions |
| P9-P10 | Service discovery, API gateway, load balancing, rate limiting questions |
| P11-P13 | Event-driven architecture, caching, resilience, observability questions |
| P14 | Docker, Kubernetes, system design, production readiness questions |

---

Every concept has a home. Every problem builds on the previous. Nothing is standalone or throwaway. Want me to start building Problem 1 in your workspace?
