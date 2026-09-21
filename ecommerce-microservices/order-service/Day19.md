# Day 19 — Order Service: RestClient + Resilience4j

## What was built

| File | Purpose |
|---|---|
| `domain/entity/Order.java` | Order entity — `@OneToMany` cascade to `OrderItem` |
| `domain/entity/OrderItem.java` | OrderItem entity — price/name snapshot at order time |
| `domain/enums/OrderStatus.java` | PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED / CANCELLED |
| `repository/OrderRepository.java` | JPA queries including `@EntityGraph`, stuck-order detection |
| `client/ProductClient.java` | Interface — contract for calling product-service |
| `client/RestClientProductClient.java` | `RestClient`-backed implementation (Spring 6.1+) |
| `client/ProductClientFallback.java` | Fallback — safe default when circuit is OPEN |
| `service/OrderService.java` | `@CircuitBreaker`, `@Retry`, `@Bulkhead` on product calls |
| `controller/OrderController.java` | REST endpoints for order CRUD |
| `exception/GlobalExceptionHandler.java` | Maps exceptions to HTTP status codes |
| `test/OrderServiceApplicationTests.java` | Context load test |
| `test/OrderRepositoryTest.java` | 6 repository tests |
| `test/OrderServiceTest.java` | 7 service tests with mocked `ProductClient` |

**Test results: 14/14 PASS**

---

## 1. Why a Separate Order Service?

Microservice principle: each service owns its data and its domain. The order-service:
- Has its own H2/MySQL database (`orderdb`)
- Runs on its own port (8083)
- Calls product-service over HTTP — never shares a database with it
- Fails gracefully when product-service is down (circuit breaker)

**No cross-service foreign keys** — `Order.userId` is just a `Long`, not a `@ManyToOne` to a `User` entity. Cross-service joins don't exist in microservices.

---

## 2. Order + OrderItem: @OneToMany Cascade

```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
private List<OrderItem> items = new ArrayList<>();
```

### Why cascade = ALL?
Saving an `Order` automatically saves all its `OrderItem`s. Deleting an `Order` deletes all its items. Without cascade, you'd have to save each item manually.

### Why orphanRemoval = true?
If you remove an item from `order.getItems()`, it gets deleted from the DB. Without this, the item row would remain as an orphan with a null FK.

### Price snapshot pattern
`OrderItem` stores `productName` and `unitPrice` at the time of ordering:
```java
this.productName = product.name();   // snapshot — product name may change later
this.unitPrice   = product.price();  // snapshot — price may change later
```
This is intentional. If a product's price changes tomorrow, existing orders must still show the price the customer paid.

---

## 3. OpenFeign vs RestClient

### What is OpenFeign?
OpenFeign is a declarative HTTP client — you define an interface and Spring generates the implementation:
```java
@FeignClient(name = "product-service", url = "${product-service.url}")
public interface ProductClient {
    @GetMapping("/api/products/{id}")
    ProductResponse getProductById(@PathVariable Long id);
}
```
Spring generates a proxy bean that makes the HTTP call when `getProductById()` is invoked.

### Why RestClient instead?
OpenFeign 4.1.0 (the cached version) is incompatible with Spring Boot 4.x — it depends on `spring-cloud-commons` which references `WebServerInitializedEvent`, a class moved in Spring Boot 4.x. The compatible version (4.3.x) requires network access to download.

`RestClient` (Spring 6.1+) is Spring's modern synchronous HTTP client — the direct replacement for `RestTemplate`. It's conceptually identical to Feign:

```java
// Feign style (declarative)
@FeignClient(name = "product-service")
public interface ProductClient {
    @GetMapping("/api/products/{id}")
    ProductResponse getProductById(@PathVariable Long id);
}

// RestClient style (programmatic, same result)
public class RestClientProductClient implements ProductClient {
    private final RestClient restClient;

    public ProductResponse getProductById(Long id) {
        return restClient.get()
                .uri("/api/products/{id}", id)
                .retrieve()
                .body(ProductResponse.class);
    }
}
```

Both approaches:
- Make HTTP GET to `http://product-service-host/api/products/{id}`
- Deserialize the JSON response into `ProductResponse`
- Throw an exception on HTTP errors

In Day 23, when Eureka is added, the URL becomes `lb://product-service` and load balancing is automatic with either approach.

---

## 4. Resilience4j — The Four Patterns

Resilience4j wraps method calls with AOP proxies. The decoration order is:

```
Bulkhead → TimeLimiter → CircuitBreaker → Retry → actual method call
```

### 4.1 Circuit Breaker

The circuit breaker is a state machine with three states:

```
CLOSED ──(failure rate > 50%)──► OPEN ──(wait 10s)──► HALF-OPEN
  ▲                                                         │
  └──────────────(3 test calls succeed)────────────────────┘
```

- **CLOSED**: normal operation, calls go through, failures are counted
- **OPEN**: circuit is tripped, calls fail immediately without hitting product-service (fast fail)
- **HALF-OPEN**: allows 3 test calls to check if product-service recovered

```java
@CircuitBreaker(name = "product-service", fallbackMethod = "getProductFallback")
public ProductResponse getProduct(Long productId) {
    return productClient.getProductById(productId);
}

// Fallback — same return type + Throwable as last param
public ProductResponse getProductFallback(Long productId, Throwable ex) {
    throw new ProductUnavailableException(productId);
}
```

Configuration:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      product-service:
        sliding-window-size: 10          # evaluate last 10 calls
        failure-rate-threshold: 50       # open if 50%+ fail
        wait-duration-in-open-state: 10s # stay OPEN for 10s before trying HALF-OPEN
        permitted-number-of-calls-in-half-open-state: 3
```

**Why circuit breaker?** Without it, if product-service is down, every order request waits for a timeout (e.g., 30s) before failing. With circuit breaker, after the threshold is hit, subsequent calls fail immediately — protecting both the caller and the downstream service from being overwhelmed.

### 4.2 Retry

```java
@Retry(name = "product-service")
public ProductResponse getProduct(Long productId) { ... }
```

```yaml
resilience4j:
  retry:
    instances:
      product-service:
        max-attempts: 3
        wait-duration: 500ms
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2   # 500ms → 1000ms → 2000ms
        retry-exceptions:
          - org.springframework.web.client.RestClientException
```

Retry fires on transient failures (network blip, temporary 503). Exponential backoff prevents hammering a struggling service.

**Retry + Circuit Breaker together**: Retry tries 3 times. If all 3 fail, the circuit breaker counts those as failures. Once the failure rate threshold is hit, the circuit opens and future calls skip the retry entirely.

### 4.3 Bulkhead

Limits concurrent calls to product-service:
```java
@Bulkhead(name = "product-service")
public ProductResponse getProduct(Long productId) { ... }
```

```yaml
resilience4j:
  bulkhead:
    instances:
      product-service:
        max-concurrent-calls: 10    # max 10 threads in product-service calls at once
        max-wait-duration: 100ms    # wait up to 100ms for a slot; reject if still full
```

**Why?** Without bulkhead, a slow product-service could exhaust all threads in the order-service thread pool, making the entire order-service unresponsive — even for requests that don't need product-service. Bulkhead isolates the blast radius.

Two bulkhead types in Resilience4j:
- **Semaphore** (used here): limits concurrent calls, same thread
- **Thread pool**: runs calls in a separate thread pool, provides true isolation

### 4.4 TimeLimiter

```yaml
resilience4j:
  timelimiter:
    instances:
      product-service:
        timeout-duration: 3s   # cancel if product-service takes > 3s
```

Cancels the call if it takes longer than the configured duration. Works with `CompletableFuture` — for synchronous `RestClient` calls, it's enforced at the Resilience4j level.

---

## 5. Fallback Strategy

```java
public ProductResponse getProductFallback(Long productId, Throwable ex) {
    log.warn("Product service unavailable for productId={}", productId);
    throw new ProductUnavailableException(productId);
}
```

The fallback method signature rules:
- Same return type as the decorated method
- Same parameters as the decorated method
- Plus `Throwable` (or a specific exception type) as the last parameter

**Fallback options** (choose based on use case):
1. **Throw exception** (our approach) — order fails with 503, user sees an error
2. **Return cached data** — return last known product info from Redis (Day 27)
3. **Return stub** — return a placeholder, order proceeds with zero price (dangerous)

For order placement, option 1 is correct — you can't place an order for an unavailable product.

---

## 6. @EntityGraph — Solving N+1

```java
@EntityGraph(attributePaths = "items")
@Query("SELECT o FROM Order o WHERE o.id = :id")
Optional<Order> findByIdWithItems(@Param("id") Long id);
```

Without `@EntityGraph`:
```sql
SELECT * FROM orders WHERE id = ?          -- 1 query
SELECT * FROM order_items WHERE order_id = ? -- N queries (one per order)
```

With `@EntityGraph`:
```sql
SELECT o.*, i.* FROM orders o
LEFT JOIN order_items i ON o.id = i.order_id
WHERE o.id = ?                             -- 1 query
```

---

## 7. Testing with @MockitoBean

```java
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
class OrderServiceTest {

    @MockitoBean(name = "restClientProductClient")
    ProductClient productClient;
```

`@MockitoBean(name = "restClientProductClient")` — replaces the `restClientProductClient` bean with a Mockito mock. The `name` is required because there's only one `ProductClient` bean now (fallback is not a Spring bean), but specifying the name is explicit and safe.

Tests mock the product client to avoid needing a real product-service running:
```java
when(productClient.getProductById(1L))
    .thenReturn(new ProductResponse(1L, "Laptop", new BigDecimal("999.99"), 10, "ELECTRONICS"));
```

---

## 8. Spring Boot 4.x Issues Encountered

| Issue | Cause | Fix |
|---|---|---|
| `spring-boot-starter-aop` version missing | Not in Spring Boot 4.1.1 BOM | Use explicit `aspectjweaver` dependency |
| OpenFeign 4.1.0 incompatible | `spring-cloud-commons` references removed `WebServerInitializedEvent` | Replace with `RestClient`-backed implementation |
| `NoUniqueBeanDefinitionException` for `ProductClient` | Both `RestClientProductClient` and `ProductClientFallback` were `@Component` | Remove `@Component` from fallback — it's used programmatically, not injected |
