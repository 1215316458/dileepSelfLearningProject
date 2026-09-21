# Day 14 — Service + Controller + Validation

---

## 1. DTO Pattern (Data Transfer Object)

Never expose your JPA entity directly from the API. Use DTOs to control what goes in and out.

```
Client → ProductRequest (DTO) → Controller → Service → Product (Entity) → DB
DB → Product (Entity) → Service → ProductResponse (DTO) → Controller → Client
```

**Why separate DTOs from entities:**
- Entity has JPA annotations, lazy-loaded relations, internal fields — not safe to expose
- Request DTO has validation annotations — entity shouldn't have those
- Response DTO can compute derived fields (e.g. `inStock`) and hide internal ones (e.g. `updatedAt`)

### ProductRequest — incoming data with validation

```java
public class ProductRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    private String name;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    @Min(value = 0, message = "Stock cannot be negative")
    private int stockQuantity;

    @NotNull
    @ValidCategory          // custom annotation
    private Category category;
}
```

### ProductResponse — outgoing data, computed from entity

```java
public class ProductResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    private boolean inStock;   // derived: stockQuantity > 0

    // static factory — converts entity to DTO, keeps conversion logic in one place
    public static ProductResponse from(Product product) {
        ProductResponse dto = new ProductResponse();
        dto.id      = product.getId();
        dto.name    = product.getName();
        dto.price   = product.getPrice();
        dto.inStock = product.getStockQuantity() > 0;
        return dto;
    }
}
```

### ApiResponse — consistent wrapper for all endpoints

```java
// Every endpoint returns ApiResponse<T> — client always gets same structure
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) { ... }
    public static <T> ApiResponse<T> error(String message) { ... }
}

// Response looks like:
// { "success": true, "message": "Success", "data": { "id": 1, "name": "Laptop" }, "timestamp": "..." }
```

---

## 2. Bean Validation (@Valid)

Spring integrates with Jakarta Bean Validation. Annotate fields, trigger with `@Valid`.

```java
// Common annotations:
@NotNull          // field must not be null
@NotBlank         // String must not be null, empty, or whitespace
@NotEmpty         // collection/String must not be null or empty
@Size(min, max)   // String/collection length bounds
@Min(value)       // number >= value
@Max(value)       // number <= value
@Positive         // number > 0
@PositiveOrZero   // number >= 0
@Digits(integer, fraction)  // decimal precision
@Email            // valid email format
@Pattern(regexp)  // matches regex
@Past / @Future   // date in past/future
```

**Triggering validation in controller:**

```java
@PostMapping
public ResponseEntity<ApiResponse<ProductResponse>> create(
        @Valid @RequestBody ProductRequest request) {  // @Valid triggers validation
    // if validation fails → MethodArgumentNotValidException thrown automatically
    // GlobalExceptionHandler catches it → returns 400 with field errors
}
```

**What happens when validation fails:**
1. Spring calls validators on each field
2. Any failure → throws `MethodArgumentNotValidException`
3. `GlobalExceptionHandler` catches it → returns 400 with field errors map

---

## 3. Custom Validator — @ValidCategory

When built-in annotations aren't enough, create your own.

**Step 1 — Define the annotation:**

```java
@Documented
@Constraint(validatedBy = CategoryValidator.class)  // points to the validator
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCategory {
    String message() default "Invalid category";
    Class<?>[] groups() default {};           // required by Bean Validation spec
    Class<? extends Payload>[] payload() default {};  // required by spec
}
```

**Step 2 — Implement the validator:**

```java
public class CategoryValidator implements ConstraintValidator<ValidCategory, Category> {

    @Override
    public boolean isValid(Category value, ConstraintValidatorContext context) {
        if (value == null) return true;  // null handled by @NotNull
        for (Category valid : Category.values()) {
            if (valid == value) return true;
        }
        return false;
    }
}
```

**Step 3 — Use it:**

```java
@NotNull
@ValidCategory
private Category category;
```

---

## 4. @Service and @Transactional

`@Service` marks a class as a business logic component. `@Transactional` manages DB transactions.

```java
@Service
@Transactional(readOnly = true)  // class-level default: all methods are read-only transactions
public class ProductService {

    // inherits readOnly = true — no write lock, better performance for reads
    public ProductResponse findById(Long id) { ... }

    // overrides class-level — this method needs a write transaction
    @Transactional
    public ProductResponse create(ProductRequest request) { ... }

    @Transactional
    public void delete(Long id) { ... }
}
```

**Why readOnly = true on reads:**
- DB can optimize: no dirty checking, no flush before query
- Some DBs route read-only transactions to read replicas
- Prevents accidental writes in read methods

**Transaction propagation (what happens when one @Transactional calls another):**

```java
// REQUIRED (default) — join existing transaction, or create new one
@Transactional(propagation = Propagation.REQUIRED)

// REQUIRES_NEW — always create a new transaction (suspends existing)
// use for audit logging — must commit even if outer transaction rolls back
@Transactional(propagation = Propagation.REQUIRES_NEW)

// SUPPORTS — join if exists, run without transaction if not
@Transactional(propagation = Propagation.SUPPORTS)
```

**Rollback rules:**
```java
// by default: rolls back on RuntimeException and Error, NOT on checked exceptions
@Transactional

// roll back on specific checked exception
@Transactional(rollbackFor = IOException.class)

// don't roll back on specific exception
@Transactional(noRollbackFor = ProductNotFoundException.class)
```

---

## 5. @Cacheable and @CacheEvict

Cache results to avoid repeated DB hits for the same data.

```java
@EnableCaching  // on main class — activates cache infrastructure
```

```java
// @Cacheable — cache the return value, skip method if cache hit
@Cacheable(value = "products", key = "#id")
public ProductResponse findById(Long id) {
    // first call: executes method, stores result in cache under key "products::1"
    // subsequent calls with same id: returns cached value, method NOT called
    return productRepository.findById(id).map(ProductResponse::from)...;
}

// @CacheEvict — remove from cache when data changes
@Transactional
@CacheEvict(value = "products", key = "#id")  // evict only this product
public ProductResponse update(Long id, ProductRequest request) { ... }

@Transactional
@CacheEvict(value = "products", allEntries = true)  // evict ALL cached products
public ProductResponse create(ProductRequest request) { ... }
```

**Cache flow:**

```
findById(1) called
  → check cache "products::1"
  → MISS: call DB, store result in cache, return
  → HIT: return cached value (no DB call)

update(1, ...) called
  → evict "products::1" from cache
  → next findById(1) will be a MISS again → fresh DB call
```

**CacheManager — Spring Boot 4.x requires explicit bean:**

```java
@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("products");
        // ConcurrentMapCacheManager = in-memory, backed by ConcurrentHashMap
        // For production: use RedisCacheManager (Day 27)
    }
}
```

---

## 6. @RestController and REST Endpoints

```java
@RestController              // = @Controller + @ResponseBody on every method
@RequestMapping("/api/products")  // base path for all methods in this class
public class ProductController {

    // GET /api/products/1
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(
            @PathVariable Long id) { ... }   // extracts {id} from URL

    // GET /api/products?page=0&size=10&sortBy=name
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAll(
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "10")   int size,
            @RequestParam(defaultValue = "name") String sortBy) { ... }

    // POST /api/products  (body: JSON ProductRequest)
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody ProductRequest request) { ... }

    // PUT /api/products/1  (body: JSON ProductRequest)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) { ... }

    // DELETE /api/products/1
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id) { ... }
}
```

**Annotation reference:**

| Annotation | Extracts from |
|-----------|--------------|
| `@PathVariable` | URL path: `/products/{id}` |
| `@RequestParam` | Query string: `?page=0&size=10` |
| `@RequestBody` | Request body (JSON → object) |
| `@RequestHeader` | HTTP header |

---

## 7. ResponseEntity

`ResponseEntity<T>` gives full control over HTTP response: status code, headers, body.

```java
// 200 OK with body
ResponseEntity.ok(body)
ResponseEntity.ok().body(body)

// 201 Created with body
ResponseEntity.status(HttpStatus.CREATED).body(body)

// 204 No Content (no body)
ResponseEntity.noContent().build()

// 404 Not Found
ResponseEntity.notFound().build()

// Custom status
ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse)
```

**Why use ResponseEntity instead of just returning the object:**
- Control HTTP status code (201 for create, 204 for delete, etc.)
- Add response headers (e.g. Location header after create)
- Return different types based on success/failure 

---

## 8. Dynamic Search with Specification

The `search()` method builds a WHERE clause dynamically based on which params are provided:

```java
public Page<ProductResponse> search(Category category, BigDecimal minPrice,
                                    BigDecimal maxPrice, Boolean inStock,
                                    String keyword, Pageable pageable) {
    Specification<Product> spec = Specification.where((Specification<Product>) null);

    if (category != null)
        spec = spec.and(ProductSpecification.hasCategory(category));
    if (minPrice != null && maxPrice != null)
        spec = spec.and(ProductSpecification.hasPriceBetween(minPrice, maxPrice));
    if (Boolean.TRUE.equals(inStock))
        spec = spec.and(ProductSpecification.inStock());
    if (keyword != null && !keyword.isBlank())
        spec = spec.and(ProductSpecification.nameContains(keyword));

    return productRepository.findAll(spec, pageable).map(ProductResponse::from);
}
```

**GET /api/products/search?category=ELECTRONICS&inStock=true&minPrice=50&maxPrice=500**
→ generates: `WHERE category = 'ELECTRONICS' AND stock_quantity > 0 AND price BETWEEN 50 AND 500`

---

## Files Created in Day 14

```
src/main/java/com/ecommerce/product_service/
├── dto/
│   ├── ProductRequest.java     incoming DTO with @Valid annotations
│   ├── ProductResponse.java    outgoing DTO with static from() factory
│   └── ApiResponse.java        generic wrapper for all responses
├── validation/
│   ├── ValidCategory.java      custom @ValidCategory annotation
│   └── CategoryValidator.java  ConstraintValidator implementation
├── exception/
│   ├── ProductNotFoundException.java
│   ├── DuplicateProductException.java
│   └── ErrorResponse.java      error body structure
├── service/
│   └── ProductService.java     @Service, @Transactional, @Cacheable
├── controller/
│   └── ProductController.java  @RestController, full REST endpoints
└── config/
    └── CacheConfig.java        explicit CacheManager bean
```
