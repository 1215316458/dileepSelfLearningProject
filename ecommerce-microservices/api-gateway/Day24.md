# Day 24 — API Gateway

## What We Built

- `api-gateway` (port 8080) — single entry point for all client requests
- JWT authentication filter — validates token, injects `X-User-Id` header downstream
- Correlation ID filter — generates UUID per request, propagates through all services
- Rate limiter — token bucket algorithm, in-memory per client IP
- CORS configuration — allows frontend (React/Angular) to call the gateway
- Routes to all 4 services via `lb://` (load-balanced via Eureka)

---

## Why an API Gateway?

Without a gateway, clients must know the address of every service:
```
POST http://order-service:8083/api/orders
GET  http://product-service:8081/api/products
POST http://user-service:8082/api/auth/login
```

Problems:
- Client must handle auth for every service separately
- CORS must be configured on every service
- Rate limiting must be implemented in every service
- No single place to add logging, tracing, or request transformation

With a gateway, clients talk to one address:
```
POST http://gateway:8080/api/orders      → routed to order-service
GET  http://gateway:8080/api/products    → routed to product-service
POST http://gateway:8080/api/auth/login  → routed to user-service
```

The gateway handles: auth, rate limiting, CORS, correlation IDs, routing.
Downstream services trust the gateway and focus on business logic.

---

## Spring Cloud Gateway Server MVC

We use `spring-cloud-starter-gateway-server-webmvc` (not the reactive WebFlux gateway)
because all downstream services are servlet-based (Spring MVC).

Routes are defined as `RouterFunction<ServerResponse>` beans:

```java
@Bean
public RouterFunction<ServerResponse> orderRoutes() {
    return GatewayRouterFunctions.route("order-service")
            .route(path("/api/orders/**"), HandlerFunctions.http())
            .filter(lb("order-service"))   // lb:// = load-balanced via Eureka
            .build();
}
```

`lb("order-service")` tells Spring Cloud LoadBalancer to:
1. Look up `order-service` in Eureka
2. Get the list of healthy instances
3. Pick one using round-robin
4. Forward the request to that instance

---

## JWT Authentication Filter

```
Client → [CorrelationIdFilter] → [RateLimitFilter] → [JwtAuthFilter] → Route → Service
```

The `JwtAuthFilter` runs on every request:
1. Skip public paths (`/api/auth/**`, `/api/products`, `/actuator`)
2. Extract `Bearer <token>` from `Authorization` header
3. Validate the JWT signature using the shared secret
4. Extract `userId` and `roles` from claims
5. Inject `X-User-Id` and `X-User-Roles` headers into the downstream request
6. Reject with 401 if token is missing or invalid

Downstream services read `X-User-Id` instead of re-parsing the JWT:
```java
@GetMapping("/profile")
public UserResponse getProfile(@RequestHeader("X-User-Id") Long userId) { ... }
```

This is the **gateway as security boundary** pattern — downstream services can be
on an internal network with no auth of their own.

---

## Correlation ID

Every request gets a UUID that flows through all services:

```
Client → Gateway (generates X-Correlation-Id: abc-123)
       → Order Service (logs with correlationId=abc-123)
       → Product Service (logs with correlationId=abc-123)
       → Response (echoes X-Correlation-Id: abc-123 back to client)
```

Log pattern:
```yaml
logging:
  pattern:
    console: "%d{HH:mm:ss} [%X{correlationId}] %-5level %logger{36} - %msg%n"
```

When something goes wrong, you search logs for the correlation ID and see the
complete request journey across all services.

---

## Rate Limiting — Token Bucket Algorithm

```
Bucket capacity: 20 tokens
Refill rate: 10 tokens/second

Request arrives → consume 1 token → allow
Request arrives → bucket empty → 429 Too Many Requests
Time passes → tokens refill → allow again
```

Why token bucket over fixed window?
- Fixed window: 100 requests allowed per minute. A burst of 100 at 00:59 + 100 at 01:00 = 200 in 2 seconds.
- Token bucket: burst is limited by bucket capacity (20). Sustained rate is limited by refill rate (10/s).

In production, the bucket state lives in Redis so all gateway instances share it.
Here we use `ConcurrentHashMap` (single instance, dev only).

---

## CORS

```java
config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:4200"));
config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
config.setAllowedHeaders(List.of("*"));
config.setAllowCredentials(true);
config.setMaxAge(3600L);  // browser caches preflight for 1 hour
```

CORS is configured once at the gateway. Downstream services don't need CORS config
because browsers only talk to the gateway, not directly to services.

---

## API Versioning

URI-based versioning (simplest, most visible):
```
/api/v1/orders  → order-service v1
/api/v2/orders  → order-service v2 (new route added to gateway)
```

The gateway routes `/api/v1/**` and `/api/v2/**` to different service instances or
different path prefixes. Services don't need to know about versioning.

---

## Load Balancing Demo

With Eureka running, start two instances of product-service on different ports:
```
java -jar product-service.jar --server.port=8081
java -jar product-service.jar --server.port=8091
```

Both register as `product-service` in Eureka. The gateway's `lb("product-service")`
alternates between them on each request (round-robin).

---

## pom.xml Changes (Day 24)

```xml
<!-- WebMVC gateway — cached at 4.3.3 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway-server-webmvc</artifactId>
</dependency>
<!-- Load balancer for lb:// routing — cached at 4.3.1 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
<!-- Eureka client — cached at 4.1.0 (pinned) -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    <version>4.1.0</version>
</dependency>
<!-- JWT for token validation in the auth filter -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
```

The gateway uses the Spring Cloud 2025.0.1 BOM. The Eureka client incompatibility
(spring-cloud-commons 4.1.0 vs SB4.x) only affects services that run tests with
a full Spring context. The gateway itself doesn't have Spring context tests, so
the binary incompatibility doesn't surface at test time.
