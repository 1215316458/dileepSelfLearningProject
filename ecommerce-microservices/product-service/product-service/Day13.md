n# Day 13 — Spring Boot: Entity + Repository + Config

---

## 1. Spring Boot Auto-Configuration

Spring Boot eliminates boilerplate XML config. When you add a dependency (starter), Spring Boot
auto-configures it based on what's on the classpath.

```java
@SpringBootApplication  // = @Configuration + @EnableAutoConfiguration + @ComponentScan
public class ProductServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
```

- `@EnableAutoConfiguration` — detects H2 on classpath → auto-creates DataSource, EntityManagerFactory, TransactionManager
- `@ComponentScan` — scans all classes in the same package and sub-packages for Spring beans
- `@Configuration` — marks this class as a source of bean definitions

**What happens on startup:**
1. Spring Boot reads `application.yml`
2. Sees H2 driver on classpath → creates in-memory DataSource
3. Sees Hibernate on classpath → creates EntityManagerFactory
4. Sees `@Entity` classes → creates tables via `ddl-auto: create-drop`
5. Sees `@Component`, `@Repository` etc → registers them as beans

---

## 2. application.yml — Externalized Configuration

`application.yml` is the central config file. YAML uses indentation (not XML tags).

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:productdb   # in-memory H2 DB named "productdb"
    driver-class-name: org.h2.Driver
    username: sa
    password:                    # empty password for H2
  jpa:
    hibernate:
      ddl-auto: create-drop      # create tables on startup, drop on shutdown
    show-sql: true               # print SQL to console
    properties:
      hibernate:
        format_sql: true         # pretty-print the SQL
  h2:
    console:
      enabled: true              # enable browser UI at /h2-console
      path: /h2-console

server:
  port: 8081                     # run on port 8081 (not default 8080)

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics   # expose these Actuator endpoints
```

**ddl-auto options:**
| Value | Behaviour |
|-------|-----------|
| `create-drop` | Create on start, drop on stop (dev/test) |
| `create` | Create on start, keep on stop |
| `update` | Alter existing tables to match entities |
| `validate` | Validate schema matches entities, fail if not |
| `none` | Do nothing (production) |

---

## 3. JPA Entity

JPA (Java Persistence API) maps Java objects to database tables.

```java
@Entity                          // marks this class as a JPA entity (maps to a table)
@Table(
    name = "products",           // explicit table name (default = class name lowercase)
    indexes = {
        @Index(name = "idx_product_category", columnList = "category"),
        @Index(name = "idx_product_name",     columnList = "name")
    }
)
@EntityListeners(AuditingEntityListener.class)  // enables @CreatedDate / @LastModifiedDate
public class Product {

    @Id                                              // primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // auto-increment (DB handles it)
    private Long id;

    @Column(nullable = false)                        // NOT NULL constraint
    private String name;

    @Column(length = 1000)                           // VARCHAR(1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)  // DECIMAL(10,2) — for money
    private BigDecimal price;

    @Column(nullable = false)
    private int stockQuantity;

    @Enumerated(EnumType.STRING)   // stores "ELECTRONICS" not 0 — safer for migrations
    @Column(nullable = false)
    private Category category;

    @CreatedDate
    @Column(nullable = false, updatable = false)   // set once on insert, never updated
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Product() {}   // JPA requires no-arg constructor (can be protected)

    // ... constructor, getters, setters
}
```

### @Enumerated: STRING vs ORDINAL

```java
// BAD — stores 0, 1, 2... If you reorder enum constants, data is corrupted
@Enumerated(EnumType.ORDINAL)
private Category category;   // ELECTRONICS=0, BOOKS=1 → reorder → data wrong

// GOOD — stores "ELECTRONICS", "BOOKS"... safe to reorder enum constants
@Enumerated(EnumType.STRING)
private Category category;
```

### @GeneratedValue strategies

```java
GenerationType.IDENTITY   // DB auto-increment (MySQL, H2) — most common
GenerationType.SEQUENCE   // DB sequence object (PostgreSQL, Oracle)
GenerationType.AUTO       // Hibernate picks based on DB — avoid, unpredictable
GenerationType.UUID       // generates UUID as primary key (Spring Boot 3+)
```

### @Index — Why it matters

Without index: `SELECT * FROM products WHERE category = 'ELECTRONICS'` → full table scan O(n)
With index: same query → B-tree lookup O(log n)

```java
@Table(indexes = {
    @Index(name = "idx_product_category", columnList = "category"),
    // composite index for queries that filter by both
    @Index(name = "idx_category_price", columnList = "category, price")
})
```

---

## 4. JPA Auditing — @CreatedDate / @LastModifiedDate

Automatically sets timestamps without you writing any code.

**3 things required:**

1. `@EnableJpaAuditing` on main class
2. `@EntityListeners(AuditingEntityListener.class)` on entity
3. `@CreatedDate` / `@LastModifiedDate` on fields

```java
// Main class
@SpringBootApplication
@EnableJpaAuditing          // activates the auditing infrastructure
public class ProductServiceApplication { ... }

// Entity
@EntityListeners(AuditingEntityListener.class)   // hooks into JPA lifecycle events
public class Product {

    @CreatedDate
    @Column(updatable = false)   // never overwrite after first insert
    private LocalDateTime createdAt;

    @LastModifiedDate            // updated on every save()
    private LocalDateTime updatedAt;
}
```

**How it works internally:**
- `AuditingEntityListener` listens to `@PrePersist` (before insert) and `@PreUpdate` (before update)
- On `@PrePersist`: sets both `createdAt` and `updatedAt`
- On `@PreUpdate`: sets only `updatedAt`

---

## 5. JpaRepository — Free CRUD Methods

`JpaRepository<Entity, ID>` gives you 18+ methods for free — no implementation needed.

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    // you get these for free:
    // save(product)           → INSERT or UPDATE
    // findById(id)            → SELECT WHERE id = ? → Optional<Product>
    // findAll()               → SELECT * FROM products
    // findAll(Pageable)       → SELECT * with LIMIT/OFFSET
    // deleteById(id)          → DELETE WHERE id = ?
    // count()                 → SELECT COUNT(*)
    // existsById(id)          → SELECT 1 WHERE id = ?
    // saveAll(list)           → batch INSERT/UPDATE
}
```

**Inheritance chain:**
```
JpaRepository
  └── PagingAndSortingRepository  (adds findAll(Pageable), findAll(Sort))
        └── CrudRepository        (adds save, findById, findAll, delete, count)
              └── Repository      (marker interface)
```

---

## 6. Derived Queries — Spring Generates SQL from Method Name

Spring Data parses the method name and generates the SQL automatically.

```java
// Spring reads: find + By + Category → SELECT * FROM products WHERE category = ?
List<Product> findByCategory(Category category);

// find + By + Name + Containing + IgnoreCase
// → SELECT * FROM products WHERE LOWER(name) LIKE LOWER('%?%')
List<Product> findByNameContainingIgnoreCase(String name);

// find + By + Price + Between
// → SELECT * FROM products WHERE price BETWEEN ? AND ?
List<Product> findByPriceBetween(BigDecimal min, BigDecimal max);

// find + By + Category + And + StockQuantity + GreaterThan
// → SELECT * FROM products WHERE category = ? AND stock_quantity > ?
List<Product> findByCategoryAndStockQuantityGreaterThan(Category category, int stock);

// find + By + Name → returns Optional (wraps null safety)
Optional<Product> findByName(String name);
```

**Keyword reference:**
| Keyword | SQL equivalent |
|---------|---------------|
| `And` | `AND` |
| `Or` | `OR` |
| `Between` | `BETWEEN ? AND ?` |
| `LessThan` | `< ?` |
| `GreaterThan` | `> ?` |
| `Like` | `LIKE ?` |
| `Containing` | `LIKE '%?%'` |
| `StartingWith` | `LIKE '?%'` |
| `IgnoreCase` | `LOWER(col) = LOWER(?)` |
| `OrderBy` | `ORDER BY col ASC/DESC` |
| `Not` | `<> ?` |
| `IsNull` | `IS NULL` |

---

## 7. @Query — JPQL and Native SQL

When derived query names get too long or complex, write the query yourself.

### JPQL (Java Persistence Query Language) — DB-agnostic

```java
// Uses entity class name (Product) and field names (price, category) — NOT table/column names
@Query("SELECT p FROM Product p WHERE p.price = (SELECT MIN(p2.price) FROM Product p2 WHERE p2.category = :category)")
Optional<Product> findCheapestInCategory(@Param("category") Category category);

// Named parameter :category matches @Param("category")
// Positional parameter ?1 matches first method argument
@Query("SELECT p FROM Product p WHERE p.category = ?1 AND p.stockQuantity > 0")
List<Product> findInStockByCategory(Category category);
```

### Native Query — raw SQL

```java
// nativeQuery = true → uses actual table/column names (products, stock_quantity)
@Query(value = "SELECT * FROM products WHERE stock_quantity = 0", nativeQuery = true)
List<Product> findOutOfStockProducts();
```

**JPQL vs Native:**
| | JPQL | Native |
|--|------|--------|
| Uses | Entity/field names | Table/column names |
| DB-agnostic | Yes | No |
| Complex SQL | Limited | Full SQL power |
| Pagination | Works automatically | Needs `countQuery` |

---

## 8. Pagination with Pageable

Instead of loading all records, load a page at a time.

```java
// Repository method — Spring handles LIMIT/OFFSET
Page<Product> findByCategory(Category category, Pageable pageable);

// Usage in DataSeeder / Service
Page<Product> page = productRepository.findByCategory(
    Category.ELECTRONICS,
    PageRequest.of(0, 2, Sort.by("price").ascending())
    //          ^page ^size  ^sort
);

page.getContent();        // List<Product> — the actual records
page.getTotalElements();  // total count across ALL pages
page.getTotalPages();     // total number of pages
page.getNumber();         // current page number (0-based)
page.isFirst();           // true if this is page 0
page.isLast();            // true if no more pages
page.hasNext();           // true if there's a next page
```

**PageRequest examples:**
```java
PageRequest.of(0, 10)                              // page 0, 10 per page, no sort
PageRequest.of(1, 10, Sort.by("name"))             // page 1, sort by name ASC
PageRequest.of(0, 10, Sort.by("price").descending()) // sort by price DESC
PageRequest.of(0, 10, Sort.by("category").and(Sort.by("price"))) // multi-sort
```

---

## 9. JPA Specification — Dynamic Filtering

When you need to build WHERE clauses dynamically (e.g. filter by optional params), use Specifications.

```java
// Repository must extend JpaSpecificationExecutor to use findAll(Specification)
public interface ProductRepository extends JpaRepository<Product, Long>,
                                           JpaSpecificationExecutor<Product> { }
```

```java
public class ProductSpecification {

    // Specification<T> is a functional interface: (Root<T>, CriteriaQuery<?>, CriteriaBuilder) -> Predicate
    // root = FROM clause (access entity fields)
    // cb   = factory for building conditions

    public static Specification<Product> hasCategory(Category category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
        //                                   ^field name as string  ^value
    }

    public static Specification<Product> hasPriceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> cb.between(root.get("price"), min, max);
    }

    public static Specification<Product> nameContains(String keyword) {
        return (root, query, cb) ->
            cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%");
    }

    public static Specification<Product> inStock() {
        return (root, query, cb) -> cb.greaterThan(root.get("stockQuantity"), 0);
    }
}
```

**Composing Specifications with .and() / .or() / .not():**

```java
// Single condition
Specification<Product> spec = ProductSpecification.hasCategory(ELECTRONICS);

// AND — both conditions must be true
Specification<Product> spec = ProductSpecification.hasCategory(ELECTRONICS)
        .and(ProductSpecification.inStock());

// OR — either condition
Specification<Product> spec = ProductSpecification.hasCategory(ELECTRONICS)
        .or(ProductSpecification.hasCategory(BOOKS));

// Chained — category=ELECTRONICS AND price between 50-500 AND in stock
Specification<Product> spec = ProductSpecification.hasCategory(ELECTRONICS)
        .and(ProductSpecification.hasPriceBetween(new BigDecimal("50"), new BigDecimal("500")))
        .and(ProductSpecification.inStock());

// Use it
List<Product> results = productRepository.findAll(spec);
Page<Product> paged   = productRepository.findAll(spec, PageRequest.of(0, 10));
```

**Why Specifications over if/else query building:**
```java
// BAD — combinatorial explosion of methods
findByCategoryAndInStock(Category c)
findByCategoryAndPriceRange(Category c, BigDecimal min, BigDecimal max)
findByCategoryAndInStockAndPriceRange(...)  // grows forever

// GOOD — compose at call site
Specification<Product> spec = Specification.where(null);
if (category != null)  spec = spec.and(hasCategory(category));
if (minPrice != null)  spec = spec.and(hasPriceBetween(minPrice, maxPrice));
if (inStockOnly)       spec = spec.and(inStock());
productRepository.findAll(spec);
```

---

## 10. HikariCP — Connection Pooling

Spring Boot auto-configures HikariCP (fastest Java connection pool) when using JPA.

**Why connection pooling?**
- Opening a DB connection is expensive (~100ms)
- Pool keeps N connections open and reuses them
- Request borrows a connection, uses it, returns it to pool

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10      # max concurrent connections (default 10)
      minimum-idle: 5            # keep 5 connections ready even when idle
      connection-timeout: 30000  # wait max 30s for a connection before failing
      idle-timeout: 600000       # close idle connections after 10 min
```

---

## 11. Spring Boot Actuator

Actuator exposes production-ready endpoints to monitor your app.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

| Endpoint | URL | What it shows |
|----------|-----|---------------|
| health | `/actuator/health` | UP/DOWN, DB status, disk space |
| info | `/actuator/info` | App name, version (from yml) |
| metrics | `/actuator/metrics` | JVM memory, HTTP requests, etc |
| beans | `/actuator/beans` | All Spring beans registered |
| env | `/actuator/env` | All environment properties |

**Health response example:**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "H2", "validationQuery": "isValid()" } },
    "diskSpace": { "status": "UP" }
  }
}
```

---

## 12. H2 Console

H2 is an in-memory database for development. Its browser UI lets you run SQL directly.

```yaml
spring:
  h2:
    console:
      enabled: true
      path: /h2-console
```

Access at: `http://localhost:8081/h2-console`
- JDBC URL: `jdbc:h2:mem:productdb`
- Username: `sa`
- Password: (empty)

You can run `SELECT * FROM PRODUCTS` to verify your seeded data.

---

## 13. Constructor Injection (No @Autowired)

Spring recommends constructor injection over field injection.

```java
// BAD — field injection (hides dependencies, can't use in tests without Spring)
@Component
public class DataSeeder {
    @Autowired
    private ProductRepository productRepository;
}

// GOOD — constructor injection (explicit, testable, immutable)
@Component
public class DataSeeder {
    private final ProductRepository productRepository;

    // Spring sees single constructor → injects automatically, no @Autowired needed
    public DataSeeder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
}
```

**Why constructor injection:**
- Dependencies are explicit and required (can't forget to set them)
- `final` field = immutable after construction
- Easy to test: `new DataSeeder(mockRepository)` — no Spring context needed

---

## 14. @PostConstruct — Run Code After Bean is Ready

```java
@Component
public class DataSeeder {

    private final ProductRepository productRepository;

    public DataSeeder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @PostConstruct   // called AFTER constructor AND dependency injection are complete
    public void seed() {
        productRepository.save(new Product(...));
    }
}
```

**Lifecycle order:**
1. Constructor called
2. Dependencies injected (productRepository set)
3. `@PostConstruct` method called ← safe to use injected dependencies here

**vs `CommandLineRunner`:**
```java
// Alternative — implements CommandLineRunner, runs after full app context is ready
@Component
public class DataSeeder implements CommandLineRunner {
    @Override
    public void run(String... args) {
        // same as @PostConstruct but runs after ALL beans are initialized
    }
}
```

---

## 15. How to Run (Important — Maven uses Java 8 by default here)

Because system Maven uses Java 8 but Spring Boot 4.1.1 requires Java 17+, you must set JAVA_HOME:

```bash
# Windows CMD
set "JAVA_HOME=C:\Program Files\Zulu\zulu-21" && mvn spring-boot:run

# Or compile only
set "JAVA_HOME=C:\Program Files\Zulu\zulu-21" && mvn compile
```

Once running:
- App: `http://localhost:8081`
- H2 Console: `http://localhost:8081/h2-console`
- Health: `http://localhost:8081/actuator/health`
- Metrics: `http://localhost:8081/actuator/metrics`

---

## Files Created in Day 13

```
product-service/src/main/
├── java/com/ecommerce/product_service/
│   ├── ProductServiceApplication.java     @SpringBootApplication + @EnableJpaAuditing
│   ├── domain/
│   │   ├── enums/
│   │   │   └── Category.java              ELECTRONICS, BOOKS, CLOTHING, SPORTS, HOME
│   │   └── entity/
│   │       └── Product.java               @Entity with auditing + indexes
│   ├── repository/
│   │   ├── ProductRepository.java         JpaRepository + JpaSpecificationExecutor
│   │   └── ProductSpecification.java      Composable Specification predicates
│   └── seeder/
│       └── DataSeeder.java                @PostConstruct seeds + tests all queries
└── resources/
    └── application.yml                    H2 config + JPA + Actuator
```
