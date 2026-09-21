# Day 23 — Service Registry (Eureka) + Config Server

## What We Built

- `service-registry` (port 8761) — Eureka Server, all microservices register here
- `config-server` (port 8888) — serves centralised config files to all services
- Config files for each service under `config-server/src/main/resources/config/`

---

## Service Registry — Why It Exists

In a microservices system, services need to call each other. You could hardcode URLs
(`http://localhost:8081`) but that breaks the moment you:
- Run multiple instances of a service (which port?)
- Deploy to a cloud environment (IPs change on restart)
- Scale up or down dynamically

A **Service Registry** solves this:
1. Each service registers itself on startup: "I am `order-service`, I'm at `192.168.1.5:8083`"
2. When `order-service` wants to call `product-service`, it asks the registry: "Where is `product-service`?"
3. The registry returns the current list of healthy instances
4. A load balancer picks one

```
order-service ──→ Eureka: "where is product-service?"
Eureka ──→ "192.168.1.5:8081, 192.168.1.6:8081"
order-service ──→ picks one, makes the call
```

---

## Eureka — How It Works

### Server (`@EnableEurekaServer`)

```java
@SpringBootApplication
@EnableEurekaServer
public class ServiceRegistryApplication { ... }
```

Activates the Eureka server endpoints:
- `GET /eureka/apps` — list all registered services
- `GET /eureka/apps/{appId}` — instances of a specific service
- Dashboard at `http://localhost:8761`

### Client (`spring-cloud-starter-netflix-eureka-client`)

Each service adds the dependency and configures:
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

On startup, the client:
1. Registers itself with its `spring.application.name` and host:port
2. Sends a heartbeat every 30 seconds (default)
3. Fetches the registry every 30 seconds so it has a local copy

### Self-Preservation Mode

```yaml
eureka:
  server:
    enable-self-preservation: false   # dev only
```

In production, if Eureka stops receiving heartbeats from many instances at once
(e.g. network partition), it assumes the network is broken — not the services.
It keeps the registrations alive instead of evicting them.

In dev, we disable this so stale instances are removed quickly.

---

## Config Server — Why It Exists

Without a Config Server, each service has its own `application.yml`. To change a
property (e.g. a feature flag, a timeout), you must:
1. Edit the file in each service
2. Rebuild and redeploy each service

With a Config Server:
1. All config lives in one place (filesystem or Git repo)
2. Services fetch their config on startup: `GET http://config-server:8888/{app}/{profile}`
3. With `@RefreshScope`, beans reload config without restart

### URL Pattern

```
GET http://localhost:8888/order-service/default
GET http://localhost:8888/order-service/dev
GET http://localhost:8888/notification-service/default
```

Config Server looks for files in this order:
1. `{application}-{profile}.yml`
2. `{application}.yml`
3. `application.yml` (shared across all services)

### `@RefreshScope`

```java
@RestController
@RefreshScope   // re-reads @Value fields when /actuator/refresh is called
public class OrderController {
    @Value("${order.max-items-per-order}")
    private int maxItems;
}
```

Trigger a reload without restart:
```
POST http://localhost:8083/actuator/refresh
```

---

## CAP Theorem

Eureka is an **AP** system (Available + Partition-tolerant):
- If the network splits, Eureka keeps serving stale data rather than refusing requests
- Consistency is sacrificed — you might get an instance that's actually down

MySQL is a **CA** system (Consistent + Available):
- In a network partition, MySQL refuses writes to maintain consistency
- Partition tolerance is sacrificed

| System | C | A | P | Trade-off |
|--------|---|---|---|-----------|
| Eureka | ✗ | ✓ | ✓ | May return stale service locations |
| MySQL | ✓ | ✓ | ✗ | Refuses writes during network split |
| Zookeeper | ✓ | ✗ | ✓ | Unavailable during network split |

**You can only pick 2 of 3.** For service discovery, availability matters more than
perfect consistency — a slightly stale registry is better than no registry at all.

---

## pom.xml Changes (Day 23)

### service-registry and config-server

Both use the Spring Cloud 2025.0.1 BOM with versions pinned to cached jars:

```xml
<dependencyManagement>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-dependencies</artifactId>
        <version>2025.0.1</version>
        <type>pom</type>
        <scope>import</scope>
    </dependency>
</dependencyManagement>
```

Config-server pins `spring-cloud-config-server` to `4.1.0` (BOM resolves `4.3.1`, not cached):
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
    <version>4.1.0</version>
</dependency>
```

### Existing services (order, notification, user, product)

**Eureka client NOT added** — `spring-cloud-commons 4.1.0` was built against Spring Boot 3.x
and references `WebServerInitializedEvent` which was removed in Spring Boot 4.x.
This causes a hard binary incompatibility during test context loading.

In a connected environment with a Spring Cloud release built for SB4.x, you would add:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

---

## Spring Boot 4.x Breaking Changes (Day 23)

| Issue | Cause | Fix |
|-------|-------|-----|
| `spring-cloud-commons 4.1.0` incompatible with SB4.1.1 | Built against SB3.x; references `WebServerInitializedEvent` removed in SB4 | Do not add Eureka client to existing services in this offline SB4.1.1 environment |
| `spring-cloud-config-server 4.3.1` not cached | BOM 2025.0.1 resolves to 4.3.1 | Pin to `4.1.0` explicitly |
