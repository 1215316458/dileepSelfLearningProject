# Day 27 — Redis Caching + Advanced Resilience + DB Optimization

## What We Built

- `CacheConfig` upgraded from `ConcurrentMapCacheManager` to `RedisCacheManager`
- `LettuceConnectionFactory` — async Redis client
- `RedisTemplate<String, Object>` — for direct Redis operations
- TTL of 5 minutes on all cached entries
- `@Profile("test")` fallback to in-memory cache (no Redis needed in tests)
- Tracing + Prometheus metrics added to product-service and order-service

---

## Cache-Aside Pattern

`@Cacheable` implements the cache-aside pattern automatically:

```
1. Request arrives for product id=5
2. Check Redis: cache hit? → return cached value (no DB call)
3. Cache miss? → call DB, store result in Redis with TTL, return result
4. On write (create/update/delete): @CacheEvict removes the entry
5. Next read fetches fresh data from DB, re-populates cache
```

```java
@Cacheable(value = "products", key = "#id")
public ProductResponse findById(Long id) { ... }   // cached

@CacheEvict(value = "products", key = "#id")
public ProductResponse update(Long id, ...) { ... }  // evicts on write
```

---

## Redis vs ConcurrentHashMap

| Dimension | Redis | ConcurrentHashMap |
|-----------|-------|-------------------|
| Scope | Shared across all service instances | Per-instance only |
| Persistence | Survives service restart (optional) | Lost on restart |
| TTL | Built-in expiry | Manual eviction only |
| Data structures | String, Hash, List, Set, SortedSet | Map only |
| Network | Requires Redis server | In-process |
| Best for | Production, multi-instance | Tests, single-instance dev |

---

## TTL — Time To Live

```java
RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(5))
        .disableCachingNullValues();
```

After 5 minutes, the entry expires automatically. Redis removes it on the next
access attempt. The next read fetches fresh data from the DB.

**Trade-off:**
- Short TTL (1 min): fresher data, more DB calls
- Long TTL (1 hour): fewer DB calls, staler data
- No TTL: data never refreshes (stale forever after first cache)

For product data that changes infrequently, 5 minutes is a good balance.

---

## Lettuce vs Jedis

Both are Redis clients for Java. Spring Boot uses Lettuce by default.

| | Lettuce | Jedis |
|--|---------|-------|
| Connection model | Single async connection, shared across threads | Connection pool (one connection per thread) |
| Thread safety | Yes — one connection handles all threads | No — each thread needs its own connection |
| Reactive support | Yes | No |
| Performance | Better under high concurrency | Better for simple single-threaded use |

Lettuce uses Netty under the hood — the same async I/O library as Spring WebFlux.

---

## pom.xml Changes (Day 27)

```xml
<!--
    spring-boot-starter-data-redis 4.1.1 is NOT cached.
    Only 3.2.0 is cached. Same offline pattern as MongoDB:
    declare spring-data-redis + lettuce-core directly at cached versions.
-->
<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-redis</artifactId>
    <version>3.2.0</version>
</dependency>
<dependency>
    <groupId>io.lettuce</groupId>
    <artifactId>lettuce-core</artifactId>
    <version>6.3.0.RELEASE</version>
</dependency>
```

Same pattern as MongoDB: the SB4.1.1 BOM points to newer versions not in the
local cache, so we declare the library directly at the cached version.

---

## Redis Data Structures (Concepts)

Beyond caching, Redis supports:

**Sorted Set** — leaderboard of most-viewed products:
```java
redisTemplate.opsForZSet().incrementScore("product:views", productId.toString(), 1);
// Get top 10:
redisTemplate.opsForZSet().reverseRange("product:views", 0, 9);
```

**String** — session storage, distributed locks:
```java
redisTemplate.opsForValue().set("session:" + token, userId, Duration.ofMinutes(15));
```

**Hash** — store object fields individually:
```java
redisTemplate.opsForHash().put("product:5", "price", "29.99");
redisTemplate.opsForHash().put("product:5", "stock", "100");
```

---

## DB Optimization Concepts

### Composite Indexes

```java
@Table(indexes = {
    @Index(columnList = "userId, status"),   // covers: findByUserIdAndStatus
    @Index(columnList = "createdAt")          // covers: ORDER BY createdAt
})
```

A composite index on `(userId, status)` covers queries that filter by both.
Without it, MySQL scans all rows for that user, then filters by status.

### @EntityGraph — Solve N+1

```java
@EntityGraph(attributePaths = {"items"})
Optional<Order> findByIdWithItems(Long id);
```

Without `@EntityGraph`: 1 query for the order + N queries for items (N+1 problem).
With `@EntityGraph`: 1 JOIN query fetches everything.

### HikariCP Tuning

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10      # max connections to DB
      minimum-idle: 5            # keep 5 connections warm
      connection-timeout: 30000  # wait 30s for a connection before failing
      idle-timeout: 600000       # close idle connections after 10 min
```

HikariCP is the default connection pool in Spring Boot. Each connection is a
TCP socket to the DB — expensive to create. The pool reuses them.

---

## Consistent Hashing (Concept)

When you shard a cache across multiple Redis nodes, you need to decide which node
stores which key. Naive approach: `node = hash(key) % numNodes`. Problem: when you
add/remove a node, almost every key moves to a different node (cache invalidation storm).

Consistent hashing: keys and nodes are placed on a ring. Each key goes to the
nearest node clockwise. Adding/removing a node only moves ~1/N of the keys.

Redis Cluster uses consistent hashing with 16,384 hash slots.
