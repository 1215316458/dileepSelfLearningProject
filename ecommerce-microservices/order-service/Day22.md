# Day 22 — Integration Testing (All Services)

## What We Built

Integration tests for Order Service that test the full HTTP → Controller → Service → Repository → H2 stack, with only the external `ProductClient` mocked.

**Test results: 25/25 pass** (14 existing + 11 new integration tests)

---

## Unit Test vs Integration Test vs E2E Test

| Type | What loads | What's mocked | Speed | Confidence |
|------|-----------|---------------|-------|------------|
| Unit | Nothing (no Spring) | Everything except the class under test | ~ms | Low — tests in isolation |
| Integration | Full Spring context | External dependencies (HTTP clients, message brokers) | ~seconds | High — tests real wiring |
| E2E | Everything including external services | Nothing (or Docker containers) | ~minutes | Highest — tests the whole system |

**Day 22 tests are integration tests** — real Spring context, real H2 database, real transaction management, real Resilience4j circuit breaker. Only `ProductClient` (the HTTP call to product-service) is mocked.

---

## `@SpringBootTest` — The Full Context

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class OrderIntegrationTest { ... }
```

`WebEnvironment.MOCK` — starts a mock servlet environment (no real HTTP port). Faster than `RANDOM_PORT` and sufficient for testing the full MVC stack.

`@ActiveProfiles("test")` — loads `application-test.yml` which configures H2 in-memory database.

### Why Not `@WebMvcTest`?

`@WebMvcTest` only loads the web layer (controllers, filters, exception handlers). It does NOT load services or repositories. You'd need to mock everything.

`@SpringBootTest` loads the entire application context — services, repositories, transaction management, Resilience4j, everything. This is what makes it an integration test.

---

## MockMvc Setup (Spring Boot 4.x)

```java
@Autowired
private WebApplicationContext context;

@BeforeEach
void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
}
```

`MockMvcBuilders.webAppContextSetup(context)` — builds MockMvc from the full Spring context, including all filters, interceptors, and exception handlers.

**Spring Boot 4.x breaking change:** `@AutoConfigureMockMvc` was removed. You must build MockMvc manually in `@BeforeEach`. This is the same pattern used in Day 18 for user-service.

---

## `@MockitoBean` — Replacing Beans in the Context

```java
@MockitoBean(name = "restClientProductClient")
private ProductClient productClient;
```

`@MockitoBean` replaces the real `RestClientProductClient` bean in the Spring context with a Mockito mock. The `name` attribute is required because two beans implement `ProductClient` (`RestClientProductClient` and `ProductClientFallback`) — without it, Spring can't determine which to replace.

**`@MockitoBean` vs `@Mock`:**
- `@Mock` — creates a Mockito mock but does NOT inject it into the Spring context
- `@MockitoBean` — creates a Mockito mock AND registers it as a Spring bean, replacing any existing bean of that type

---

## What Each Test Covers

### Happy Path

```java
@Test
void placeOrderSimple_returns201WithOrderDetails() throws Exception {
    when(productClient.getProductById(10L)).thenReturn(stubProduct(10L, 100));

    mockMvc.perform(post("/api/orders/simple")
                    .contentType(APPLICATION_JSON)
                    .content(mapper.writeValueAsString(TestDataFactory.validOrderRequest())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"));
}
```

Tests the full stack: HTTP POST → `OrderController` → `OrderService.placeOrder()` → `OrderRepository.save()` → H2 → response.

### Validation

```java
@Test
void placeOrder_emptyItems_returns400() throws Exception {
    String badRequest = """
            { "userId": 1, "shippingAddress": "123 Test St", "items": [] }
            """;
    mockMvc.perform(post("/api/orders/simple")
                    .contentType(APPLICATION_JSON)
                    .content(badRequest))
            .andExpect(status().isBadRequest());
}
```

Tests that `@NotEmpty` on `items` triggers `MethodArgumentNotValidException` → `GlobalExceptionHandler` → 400.

### Circuit Breaker Fallback

```java
@Test
void placeOrder_productServiceDown_returnsError() throws Exception {
    when(productClient.getProductById(anyLong()))
            .thenThrow(new RuntimeException("Connection refused"));

    mockMvc.perform(post("/api/orders/simple")
                    .contentType(APPLICATION_JSON)
                    .content(mapper.writeValueAsString(TestDataFactory.validOrderRequest())))
            .andExpect(status().is5xxServerError());
}
```

Tests the Resilience4j circuit breaker: mock throws `RuntimeException` → Retry retries 3 times → Circuit breaker fires fallback → `ProductUnavailableException` → 5xx response.

**Note:** The exact status code (503 vs 500) depends on whether Resilience4j wraps the exception before the `GlobalExceptionHandler` sees it. We use `is5xxServerError()` to be resilient to this implementation detail.

### 404 for Missing Resources

```java
@Test
void getOrder_notFound_returns404() throws Exception {
    mockMvc.perform(get("/api/orders/99999"))
            .andExpect(status().isNotFound());
}
```

Tests that `OrderNotFoundException` → `GlobalExceptionHandler` → 404.

### State Transitions

```java
@Test
void updateStatus_existingOrder_returnsUpdatedStatus() throws Exception {
    // Create order first
    String createResponse = mockMvc.perform(post("/api/orders/simple")...)
            .andReturn().getResponse().getContentAsString();
    Long orderId = mapper.readTree(createResponse).get("id").asLong();

    // Then update status
    mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                    .param("status", "CONFIRMED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
}
```

Tests a multi-step flow: create → update → verify. The H2 database persists state between requests within the same test.

---

## TestDataFactory

```java
public class TestDataFactory {
    public static PlaceOrderRequest validOrderRequest() {
        return new PlaceOrderRequest(1L, "123 Test Street",
                List.of(new PlaceOrderRequest.OrderItemRequest(10L, 2)));
    }
}
```

Centralises test data creation. Benefits:
- Tests stay readable — `TestDataFactory.validOrderRequest()` is self-documenting
- Change the DTO structure in one place, not in every test
- Encourages meaningful names (`validOrderRequest`, `multiItemOrderRequest`)

---

## WireMock — What It Is and Why We Didn't Use It

**WireMock** is an HTTP server that you configure to return specific responses. It's used to stub external HTTP services in integration tests.

```java
// WireMock example (not used — not in local cache)
@WireMockTest
class OrderIntegrationTest {
    @Test
    void placeOrder_productServiceDown() {
        stubFor(get(urlEqualTo("/api/products/10"))
                .willReturn(aResponse().withStatus(500)));
        // ...
    }
}
```

**Why we used `@MockitoBean` instead:**
- WireMock is not in the local Maven cache (offline environment)
- `@MockitoBean` achieves the same goal: isolate the external HTTP dependency
- `@MockitoBean` is simpler — no HTTP server to start/stop, no port conflicts

**When WireMock is better than `@MockitoBean`:**
- When you want to test HTTP-level behaviour (headers, timeouts, connection errors)
- When the client is not a Spring bean (e.g., a raw `HttpClient`)
- When you want to verify the exact HTTP request that was sent

---

## TestContainers — What It Is and Why We Didn't Use It

**TestContainers** starts real Docker containers (MySQL, MongoDB, Kafka, etc.) during tests.

```java
// TestContainers example (not used — not in local cache)
@SpringBootTest
@Testcontainers
class OrderIntegrationTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
    }
}
```

**Why we used H2 instead:**
- TestContainers jars not in local Maven cache (offline environment)
- Requires Docker to be running
- H2 in-memory is sufficient for testing SQL logic
- H2 is faster (no container startup time)

**When TestContainers is better than H2:**
- When you need MySQL-specific behaviour (JSON columns, full-text search, stored procedures)
- When you want to test database migrations (Flyway/Liquibase)
- When H2 compatibility mode doesn't cover your queries

---

## Test Profiles and `application-test.yml`

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testorderdb;DB_CLOSE_DELAY=-1
  jpa:
    hibernate:
      ddl-auto: create-drop

product-service:
  url: http://localhost:8081

resilience4j:
  retry:
    instances:
      product-service:
        max-attempts: 3
        wait-duration: 100ms  # Fast retries in tests
```

`DB_CLOSE_DELAY=-1` — keeps the H2 database alive for the duration of the JVM (not just the connection). Without this, the database is dropped when the first connection closes.

`ddl-auto: create-drop` — creates the schema on startup, drops it on shutdown. Each test run starts with a clean schema.

`wait-duration: 100ms` — retries happen quickly in tests (vs 500ms in production). This prevents tests from being slow.

---

## Integration Test vs Unit Test — Which to Write When

| Scenario | Write | Reason |
|----------|-------|--------|
| Business logic (calculations, state machines) | Unit test | Fast, focused, no infrastructure |
| Repository queries (JPQL, derived queries) | Integration test | Need real DB to verify SQL |
| Controller validation | Integration test | Need full MVC stack |
| Service + repository wiring | Integration test | Verify Spring wiring is correct |
| External HTTP calls | Unit test with mock | Don't want real network in tests |
| Transaction rollback | Integration test | Need real transaction manager |
| Circuit breaker behaviour | Integration test | Need real Resilience4j AOP |

**Rule of thumb:** Write unit tests for logic, integration tests for wiring and infrastructure.

---

## pom.xml Changes Explained

Order-service had only one pom.xml fix. Everything else uses standard starters that resolve cleanly from the SB4.1.1 BOM.

### Fix 1 — Pin aspectjweaver to the cached version

**Problem:** Resilience4j uses Spring AOP to intercept `@CircuitBreaker` and `@Retry` annotated methods. AOP proxy weaving requires `aspectjweaver` on the classpath. The SB4.1.1 BOM manages `aspectjweaver` at version `1.9.25.1`, but that version was not in the local Maven cache. Without it, the circuit breaker annotations silently do nothing — no retries, no fallback, no exception wrapping.

**What was changed:**
```xml
<!-- ADDED — pinned to the cached version instead of BOM's 1.9.25.1: -->
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
    <version>1.9.22.1</version>
</dependency>
```

The explicit `<version>` overrides the BOM. `1.9.22.1` is fully compatible with Resilience4j 2.1.0 and Spring Boot 4.x.

### Why other services needed no pom.xml fixes

| Service | pom.xml status | Reason |
|---------|---------------|--------|
| order-service | 1 fix (aspectjweaver pin) | BOM version not cached; needed for Resilience4j AOP |
| user-service | No fixes | All starters (web, jpa, security, validation) cached at SB4.1.1 |
| product-service | No fixes | All starters (web, jpa, validation) cached at SB4.1.1 |
| notification-service | 3 fixes | MongoDB starter not cached; driver version conflict; commons-logging removed in Spring 7.x |

The pattern: **standard Spring Boot starters work fine offline** because they were cached when the project was first set up. Problems only arise when a library is either (a) not a standard starter, or (b) the BOM points to a version that was never downloaded.

---

## Spring Boot 4.x Breaking Changes (Day 22)

| Issue | Cause | Fix |
|-------|-------|-----|
| `@AutoConfigureMockMvc` removed | Spring Boot 4.x restructured test autoconfiguration | Use `MockMvcBuilders.webAppContextSetup(context).build()` in `@BeforeEach` |
| `ObjectMapper` not a bean in `WebEnvironment.MOCK` | Spring Boot 4.x uses `tools.jackson` (Jackson 3.x), not `com.fasterxml.jackson` | Instantiate `new ObjectMapper()` directly; import from `tools.jackson.databind` |
| `@MockitoBean` ambiguity | Multiple beans implement same interface | Specify `name` attribute: `@MockitoBean(name = "restClientProductClient")` |
| Circuit breaker returns 500 instead of 503 | Resilience4j wraps `ProductUnavailableException` before `GlobalExceptionHandler` sees it | Use `is5xxServerError()` instead of `isServiceUnavailable()` in tests |

---

## End-to-End Flow (Conceptual)

The full user journey across all services built so far:

```
1. POST /api/auth/register (user-service:8082)
   → Creates user, returns JWT tokens

2. POST /api/auth/login (user-service:8082)
   → Returns access token (15min) + refresh token (7 days)

3. GET /api/products (product-service:8081)
   → Browse products (no auth required)

4. POST /api/orders/simple (order-service:8083)
   Authorization: Bearer <access_token>
   → Calls product-service to validate stock
   → Creates order in H2
   → Fires async confirmation
   → Returns order with PENDING status

5. (Day 25) Order Service publishes OrderPlacedEvent to Kafka
   → Notification Service consumes event
   → Creates Notification document in MongoDB

6. GET /api/notifications/user/{userId} (notification-service:8084)
   → Returns list of notifications for the user
```

This is the microservices architecture in action — each service owns its data, communicates through well-defined APIs, and can be deployed independently.
