# Day 15 — Exception Handling + AOP + Actuator

---

## 1. @RestControllerAdvice — Global Exception Handler

Without a global handler, Spring returns a generic 500 error for every exception.
`@RestControllerAdvice` intercepts exceptions from ALL controllers in one place.

```java
@RestControllerAdvice  // = @ControllerAdvice + @ResponseBody
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ProductNotFoundException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
            404, "Not Found", ex.getMessage(), request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        // collect field errors: { "name": "Name is required", "price": "Must be positive" }
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid",
                (existing, duplicate) -> existing  // keep first error per field
            ));
        ErrorResponse error = new ErrorResponse(400, "Validation Failed", "...", request.getRequestURI());
        error.setFieldErrors(fieldErrors);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)  // catch-all — must be last
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        // log ex here — don't expose internal details to client
        return ResponseEntity.internalServerError()
            .body(new ErrorResponse(500, "Internal Server Error", "An unexpected error occurred", request.getRequestURI()));
    }
}
```

**Exception → HTTP status mapping:**

| Exception | HTTP Status |
|-----------|------------|
| `ProductNotFoundException` | 404 Not Found |
| `DuplicateProductException` | 409 Conflict |
| `MethodArgumentNotValidException` | 400 Bad Request |
| `Exception` (catch-all) | 500 Internal Server Error |

**ErrorResponse structure:**
```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "Request validation failed",
  "path": "/api/products",
  "timestamp": "2024-01-15T10:30:00",
  "fieldErrors": {
    "name": "Name is required",
    "price": "Price must be positive"
  }
}
```

---

## 2. AOP — Aspect-Oriented Programming

AOP separates cross-cutting concerns (logging, timing, security) from business logic.
Without AOP you'd add logging to every service method manually.

**Core concepts:**

| Term | Meaning |
|------|---------|
| **Aspect** | The class containing cross-cutting logic (`@Aspect`) |
| **Advice** | The method that runs (`@Around`, `@Before`, `@After`, etc.) |
| **Pointcut** | Expression that selects which methods to intercept |
| **JoinPoint** | The actual method being intercepted |
| **Weaving** | Process of applying aspects to target code |

```java
@Aspect
@Component
public class LoggingAspect {

    // pointcut — reusable expression: all methods in service package
    @Pointcut("execution(* com.ecommerce.product_service.service.*.*(..))")
    public void serviceLayer() {}
    //          ^return  ^package                                ^any args

    // pointcut — methods annotated with @TrackExecutionTime
    @Pointcut("@annotation(com.ecommerce.product_service.aspect.TrackExecutionTime)")
    public void trackExecutionTime() {}
}
```

---

## 3. AOP Advice Types

### @Around — most powerful, wraps the method

```java
@Around("serviceLayer()")
public Object logServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {
    String method = joinPoint.getSignature().toShortString();
    String args   = Arrays.toString(joinPoint.getArgs());

    log.info("→ Calling {} with args: {}", method, args);
    long start = System.currentTimeMillis();

    Object result = joinPoint.proceed();  // MUST call this to execute the actual method

    long duration = System.currentTimeMillis() - start;
    log.info("← {} completed in {}ms", method, duration);
    return result;
}
```

`@Around` can:
- Run code before AND after the method
- Modify arguments before passing to method
- Modify return value
- Suppress the method call entirely
- Catch and handle exceptions

### @Before — runs before the method

```java
@Before("serviceLayer()")
public void logBefore(JoinPoint joinPoint) {
    log.info("About to call: {}", joinPoint.getSignature().getName());
    // cannot stop method execution, cannot modify return value
}
```

### @AfterReturning — runs after successful return

```java
@AfterReturning(pointcut = "serviceLayer()", returning = "result")
public void logAfterReturn(JoinPoint joinPoint, Object result) {
    log.info("{} returned: {}", joinPoint.getSignature().getName(), result);
}
```

### @AfterThrowing — runs when method throws exception

```java
@AfterThrowing(pointcut = "serviceLayer()", throwing = "ex")
public void logException(JoinPoint joinPoint, Exception ex) {
    log.error("✗ Exception in {}: {}", joinPoint.getSignature().toShortString(), ex.getMessage());
    // does NOT suppress the exception — it still propagates
}
```

### @After — runs always (like finally)

```java
@After("serviceLayer()")
public void logAfter(JoinPoint joinPoint) {
    log.info("Finished: {}", joinPoint.getSignature().getName());
    // runs whether method succeeded or threw exception
}
```

---

## 4. Pointcut Expressions

```java
// all methods in a package
"execution(* com.ecommerce.product_service.service.*.*(..))"
//          ^any return type  ^package         ^any class ^any method ^any args

// specific method
"execution(* com.ecommerce.product_service.service.ProductService.findById(..))"

// methods with specific annotation
"@annotation(com.ecommerce.product_service.aspect.TrackExecutionTime)"

// methods in classes with specific annotation
"@within(org.springframework.stereotype.Service)"

// combine with && || !
"serviceLayer() && !execution(* *.find*(..))"  // service methods that don't start with "find"
```

---

## 5. Custom Annotation — @TrackExecutionTime

Create your own annotation to mark specific methods for timing.

```java
// Step 1 — define the annotation
@Documented
@Target(ElementType.METHOD)         // only on methods
@Retention(RetentionPolicy.RUNTIME) // available at runtime for reflection
public @interface TrackExecutionTime {
}

// Step 2 — aspect picks it up via pointcut
@Around("@annotation(com.ecommerce.product_service.aspect.TrackExecutionTime)")
public Object trackTime(ProceedingJoinPoint joinPoint) throws Throwable {
    long start  = System.currentTimeMillis();
    Object result = joinPoint.proceed();
    long duration = System.currentTimeMillis() - start;
    log.info("[TRACK] {} executed in {}ms", joinPoint.getSignature().toShortString(), duration);
    return result;
}

// Step 3 — use it on any method
@TrackExecutionTime
public ProductResponse findById(Long id) { ... }
```

---

## 6. How AOP Works Internally (Proxy Pattern)

Spring AOP uses **proxies** — it wraps your bean in a proxy object.

```
Client calls productService.findById(1)
  → actually calls LoggingAspect proxy
    → proxy runs @Around before code
    → proxy calls real productService.findById(1)
    → proxy runs @Around after code
  → returns result to client
```

Two proxy types:
- **CGLIB proxy** (default for classes) — subclasses your class at runtime
- **JDK dynamic proxy** (for interfaces) — implements the interface at runtime

**Limitation:** AOP only intercepts calls from OUTSIDE the bean.
```java
// this.findById() inside ProductService — AOP does NOT intercept (self-invocation)
// productService.findById() from another bean — AOP DOES intercept
```

---

## 7. Spring Boot Actuator

Actuator exposes production-ready monitoring endpoints.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,beans,env
  endpoint:
    health:
      show-details: always
```

**Key endpoints:**

| Endpoint | URL | Purpose |
|----------|-----|---------|
| `/actuator/health` | Health check | UP/DOWN, DB status, disk space |
| `/actuator/info` | App info | Name, version from yml |
| `/actuator/metrics` | Metrics list | All available metric names |
| `/actuator/metrics/jvm.memory.used` | Specific metric | JVM memory usage |
| `/actuator/beans` | All beans | Every Spring bean registered |
| `/actuator/env` | Environment | All properties and their sources |
| `/actuator/mappings` | URL mappings | All `@RequestMapping` routes |

**Health response:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": { "database": "H2", "validationQuery": "isValid()" }
    },
    "diskSpace": {
      "status": "UP",
      "details": { "total": 499963174912, "free": 72334651392, "threshold": 10485760 }
    }
  }
}
```

**Info endpoint — populate from yml:**
```yaml
info:
  app:
    name: product-service
    version: 1.0.0
    description: Product management microservice
```

**Metrics — access specific metric:**
```
GET /actuator/metrics/http.server.requests
→ shows count, total time, max time for all HTTP requests

GET /actuator/metrics/jvm.memory.used
→ shows current JVM heap usage
```

---

## 8. Files Created in Day 15

```
src/main/java/com/ecommerce/product_service/
├── exception/
│   ├── GlobalExceptionHandler.java   @RestControllerAdvice
│   └── ErrorResponse.java            error body structure
└── aspect/
    ├── TrackExecutionTime.java        custom annotation
    └── LoggingAspect.java             @Aspect with @Around, @AfterThrowing
```
