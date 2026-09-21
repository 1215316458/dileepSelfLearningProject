# Day 16 — Profiles + Testing

---

## 1. Spring Profiles

Profiles let you have different configurations for different environments without changing code.

```yaml
# application.yml — base config (always loaded)
spring:
  profiles:
    active: dev   # which profile is active

# H2 in-memory for dev, MySQL for prod
spring:
  datasource:
    url: jdbc:h2:mem:productdb
```

**Profile-specific yml files:**

```
application.yml          → always loaded (base config)
application-dev.yml      → loaded when profile = dev
application-prod.yml     → loaded when profile = prod
application-test.yml     → loaded when profile = test
```

Profile-specific values override base values:

```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:h2:mem:productdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  h2:
    console:
      enabled: true

# application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ecommerce
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: ${DB_USER}      # from environment variable
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate      # never auto-create in prod
    show-sql: false
```

**Activating profiles:**

```bash
# via yml
spring.profiles.active: dev

# via command line (overrides yml)
java -jar app.jar --spring.profiles.active=prod

# via environment variable
SPRING_PROFILES_ACTIVE=prod java -jar app.jar
```

**@Profile on beans — only create bean for specific profile:**

```java
@Component
@Profile("dev")   // only registered as a bean when profile = dev
public class DataSeeder {
    @PostConstruct
    public void seed() { ... }
}

// prod profile gets no DataSeeder — no test data in production
```

---

## 2. Testing Pyramid

```
        /\
       /  \   E2E Tests (few, slow, test full system)
      /----\
     /      \  Integration Tests (some, medium speed)
    /--------\
   /          \ Unit Tests (many, fast, test one class)
  /____________\
```

- **Unit tests** — test one class in isolation, mock all dependencies
- **Integration tests** — test multiple layers together with real DB/context
- **E2E tests** — test full user journey through HTTP

---

## 3. Unit Testing with Mockito

Test the service layer in isolation — no Spring context, no DB, just pure Java.

```java
@ExtendWith(MockitoExtension.class)  // activates Mockito — no Spring needed
class ProductServiceTest {

    @Mock
    ProductRepository productRepository;  // fake — records calls, returns configured values

    @InjectMocks
    ProductService productService;        // real service, mocked repo injected via constructor
```

**Mockito core methods:**

```java
// when().thenReturn() — configure what mock returns
when(productRepository.findById(1L)).thenReturn(Optional.of(product));

// when().thenThrow() — configure mock to throw
when(productRepository.findById(99L)).thenThrow(new RuntimeException("not found"));

// verify() — assert a method was called
verify(productRepository, times(1)).findById(1L);
verify(productRepository, never()).save(any());   // assert save was NOT called

// ArgumentMatchers
when(productRepository.save(any(Product.class))).thenReturn(product);  // any Product
when(productRepository.findById(eq(1L))).thenReturn(...);              // exact value
```

**AssertJ assertions (fluent, readable):**

```java
// basic
assertThat(response.getName()).isEqualTo("Laptop");
assertThat(response).isNotNull();
assertThat(list).hasSize(3);
assertThat(list).isEmpty();

// exception assertion
assertThatThrownBy(() -> productService.findById(99L))
    .isInstanceOf(ProductNotFoundException.class)
    .hasMessageContaining("99");

// collection assertions
assertThat(list).allMatch(p -> p.getCategory() == Category.ELECTRONICS);
assertThat(list).anyMatch(p -> p.getName().equals("Laptop"));
```

**Full test example:**

```java
@Test
void findById_existingId_returnsResponse() {
    // ARRANGE — set up mock behaviour
    Product product = new Product("Laptop", "desc", new BigDecimal("999"), 5, Category.ELECTRONICS);
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    // ACT — call the method under test
    ProductResponse response = productService.findById(1L);

    // ASSERT — verify the result
    assertThat(response.getName()).isEqualTo("Laptop");
    assertThat(response.getCategory()).isEqualTo(Category.ELECTRONICS);
    verify(productRepository, times(1)).findById(1L);
}

@Test
void create_duplicateName_throwsDuplicateProductException() {
    when(productRepository.findByName("Laptop")).thenReturn(Optional.of(product));

    assertThatThrownBy(() -> productService.create(request))
        .isInstanceOf(DuplicateProductException.class);

    verify(productRepository, never()).save(any());  // save must NOT be called
}
```

---

## 4. Controller Testing with MockMvc

Test the controller layer — HTTP request/response, validation, status codes.
No real HTTP server — MockMvc simulates requests in-process.

**Spring Boot 4.x approach — manual MockMvc setup (no @WebMvcTest):**

```java
@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    ProductService productService;

    @InjectMocks
    ProductController productController;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // build MockMvc manually — include GlobalExceptionHandler so error responses work
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
```

**Making requests and asserting responses:**

```java
// GET request
mockMvc.perform(get("/api/products/1"))
    .andExpect(status().isOk())                          // HTTP 200
    .andExpect(jsonPath("$.success").value(true))        // JSON field check
    .andExpect(jsonPath("$.data.name").value("Laptop")); // nested JSON field

// POST request with body
mockMvc.perform(post("/api/products")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
    .andExpect(status().isCreated())   // HTTP 201
    .andExpect(jsonPath("$.data.id").exists());

// DELETE request
mockMvc.perform(delete("/api/products/1"))
    .andExpect(status().isOk());

// Validation failure
mockMvc.perform(post("/api/products")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\": \"\"}"))   // blank name
    .andExpect(status().isBadRequest())
    .andExpect(jsonPath("$.fieldErrors.name").exists());
```

**jsonPath syntax:**

```java
jsonPath("$.success")           // top-level field
jsonPath("$.data.name")         // nested field
jsonPath("$.data.id").value(1)  // field equals value
jsonPath("$.fieldErrors.name").exists()  // field exists
jsonPath("$.data").isArray()    // field is array
jsonPath("$.data[0].name")      // first array element
```

---

## 5. Repository Testing with @SpringBootTest + @Transactional

Test repository queries against a real H2 database.

```java
@SpringBootTest   // loads full Spring context with real H2 DB
@Transactional    // rolls back after each test — keeps DB clean between tests
class ProductRepositoryTest {

    @Autowired
    ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();  // clean slate
        productRepository.save(new Product("Laptop", ...));
        productRepository.save(new Product("Mouse", ...));
    }

    @Test
    void findByCategory_returnsMatchingProducts() {
        List<Product> result = productRepository.findByCategory(Category.ELECTRONICS);
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(p -> p.getCategory() == Category.ELECTRONICS);
    }
}
```

**Why @Transactional on test class:**
- Each test runs in a transaction
- After test completes → transaction is rolled back
- Next test starts with clean DB
- No need to manually clean up between tests

---

## 6. Spring Boot 4.x Testing Changes

Spring Boot 4.x removed several test annotations that existed in 3.x:

| Spring Boot 3.x | Spring Boot 4.x |
|----------------|----------------|
| `@MockBean` | `@MockitoBean` (from Spring Framework) |
| `@SpyBean` | `@MockitoSpyBean` |
| `@WebMvcTest` | Removed — use `@SpringBootTest` or manual MockMvc |
| `@DataJpaTest` | Removed — use `@SpringBootTest` + `@Transactional` |
| `@AutoConfigureMockMvc` | Removed — use `MockMvcBuilders.standaloneSetup()` |
| `com.fasterxml.jackson` | `tools.jackson` (Jackson 3.x) |

**Why these were removed:**
Spring Boot 4.x moved toward simpler, more explicit test setup rather than magic annotations that load partial contexts.

---

## 7. Test Naming Convention

```java
// pattern: methodName_condition_expectedBehaviour
void findById_existingId_returnsResponse()
void findById_missingId_throwsProductNotFoundException()
void create_duplicateName_throwsDuplicateProductException()
void create_validRequest_savesAndReturnsResponse()
void delete_existingId_deletesProduct()
```

This makes test failures immediately clear — you know exactly what broke and why.

---

## 8. Files Created in Day 16

```
src/test/java/com/ecommerce/product_service/
├── service/
│   └── ProductServiceTest.java      Mockito unit tests — no Spring context
├── controller/
│   └── ProductControllerTest.java   MockMvc controller tests — manual setup
└── repository/
    └── ProductRepositoryTest.java   @SpringBootTest + @Transactional — real H2
```

---

## 9. Spring Boot 4.x Specific Notes

- Jackson 3.x uses `tools.jackson.databind.ObjectMapper` (not `com.fasterxml.jackson`)
- `@MockitoBean` is in `org.springframework.test.context.bean.override.mockito`
- `CacheManager` must be explicitly declared as a `@Bean` — not auto-configured from yml
- AOP starters (`spring-boot-starter-aop`) don't exist — use `spring-aspects` + `aspectjweaver` directly
- `Specification.where(null)` is ambiguous — cast: `Specification.where((Specification<Product>) null)`
