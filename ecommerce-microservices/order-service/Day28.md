# Day 28 — Observability: Tracing + Metrics + Logging

## What We Built

- `micrometer-tracing-bridge-brave` + `zipkin-reporter-brave` added to order-service and product-service
- `OrderMetrics` — custom Counter, Timer, Gauge using Micrometer
- Tracing config: 100% sampling, Zipkin endpoint at `http://localhost:9411`
- Log pattern includes `traceId` and `spanId` from MDC
- Prometheus metrics endpoint at `/actuator/metrics` and `/actuator/prometheus`

---

## The Three Pillars of Observability

| Pillar | What it answers | Tool |
|--------|----------------|------|
| Logs | What happened? | SLF4J + Logback |
| Metrics | How much / how fast? | Micrometer + Prometheus |
| Traces | Where did the time go? | Zipkin + Brave |

You need all three. Logs tell you what happened but not where time was spent.
Metrics tell you latency but not which specific request was slow. Traces connect
a single request across all services.

---

## Distributed Tracing

A single user request touches multiple services:
```
Browser → Gateway → Order Service → Product Service → DB
```

Without tracing, you have separate logs in each service with no way to connect them.

With tracing:
- Every request gets a `traceId` (UUID, same across all services)
- Each service creates a `spanId` (UUID, unique per service hop)
- Spans are sent to Zipkin which visualises the full request tree

```
traceId: abc-123
  span: gateway (5ms)
  span: order-service (120ms)
    span: product-service call (80ms)
      span: DB query (15ms)
```

You can see exactly where the 120ms was spent.

### Configuration

```yaml
management:
  tracing:
    sampling:
      probability: 1.0   # sample 100% of requests (use 0.1 in production)
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

`probability: 1.0` — trace every request. In production, use `0.1` (10%) to
reduce overhead. High-traffic services can't afford to trace every request.

### Log Pattern with Trace IDs

```yaml
logging:
  pattern:
    console: "%d{HH:mm:ss} [%X{traceId}/%X{spanId}] %-5level %logger{36} - %msg%n"
```

`%X{traceId}` reads from MDC (Mapped Diagnostic Context) — a thread-local map
that Micrometer Tracing populates automatically. Every log line for a request
includes the trace ID, so you can grep logs across services.

---

## Micrometer Metrics

Micrometer is the metrics facade for Spring Boot — like SLF4J but for metrics.
You write `Counter.increment()` and Micrometer sends it to Prometheus, Datadog,
CloudWatch, etc. depending on which backend you configure.

### Counter

```java
Counter.builder("orders.placed")
       .description("Total number of orders placed")
       .tag("service", "order-service")
       .register(registry);

ordersPlacedCounter.increment();
```

Monotonically increasing. Never decreases. Use for: events that happen.

Prometheus query: `rate(orders_placed_total[5m])` → orders per second over 5 min.

### Timer

```java
Timer.builder("orders.processing.time")
     .description("Time taken to process an order")
     .register(registry);

return metrics.orderProcessingTimer().record(() -> {
    // ... placeOrder logic ...
});
```

Automatically tracks: count, sum, max, percentiles (p50, p95, p99).

Prometheus query: `histogram_quantile(0.95, orders_processing_time_seconds_bucket)`
→ 95th percentile latency.

### Gauge

```java
AtomicInteger activeOrders = new AtomicInteger(0);
Gauge.builder("orders.active", activeOrders, AtomicInteger::get)
     .description("Current number of active orders")
     .register(registry);
```

Current value that can go up or down. Prometheus reads it on every scrape.
Use for: queue depth, connection pool size, active sessions.

---

## Prometheus + Grafana (Concept)

```
Service → /actuator/prometheus → Prometheus scrapes every 15s → Grafana visualises
```

Prometheus stores time-series data. Grafana queries Prometheus and renders dashboards.

Example Grafana panels:
- Orders per second (Counter rate)
- P95 order processing latency (Timer histogram)
- Active orders right now (Gauge)
- Error rate (Counter rate of `orders.errors`)

---

## Health Groups (Readiness / Liveness)

```yaml
management:
  endpoint:
    health:
      group:
        readiness:
          include: db, redis, kafka
        liveness:
          include: ping
```

Kubernetes uses two probes:
- **Liveness**: is the process alive? If not, restart it. (`/actuator/health/liveness`)
- **Readiness**: is the service ready to receive traffic? If not, remove from load balancer. (`/actuator/health/readiness`)

A service can be alive (process running) but not ready (DB connection lost).
Kubernetes should stop sending traffic but not restart the pod.

---

## ELK Stack (Concept)

In production, logs from all services are aggregated:

```
Service logs → Logstash (collect + parse) → Elasticsearch (store + index) → Kibana (search + visualise)
```

You search Kibana for `traceId: abc-123` and see all log lines from all services
for that request, in chronological order.

We don't implement ELK here — it requires running Elasticsearch and Kibana.
The correlation ID and traceId in our log pattern are the foundation for ELK integration.

---

## pom.xml Changes (Day 28)

Added to order-service and product-service:
```xml
<!--
    micrometer-tracing-bridge-brave 1.2.0 + zipkin-reporter-brave 2.16.3 are cached.
    bridge-brave connects Micrometer Tracing API to Brave (Zipkin's Java tracer).
    zipkin-reporter-brave sends spans to the Zipkin server.
-->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
    <version>1.2.0</version>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
    <version>2.16.3</version>
</dependency>
```

These are pinned because the SB4.1.1 BOM manages micrometer-tracing at a newer
version that may not be cached.
