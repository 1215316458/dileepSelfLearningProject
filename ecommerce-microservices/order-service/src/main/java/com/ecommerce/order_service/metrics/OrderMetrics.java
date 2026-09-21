package com.ecommerce.order_service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * OrderMetrics — custom Micrometer metrics for the order service.
 *
 * Three metric types used here:
 *
 * Counter — monotonically increasing count (never decreases)
 *   Use for: events that happen (orders placed, errors, logins)
 *   Query: rate(orders_placed_total[5m]) → orders per second over last 5 min
 *
 * Timer — measures duration + count of operations
 *   Use for: how long something takes (order processing, DB queries)
 *   Automatically tracks: count, sum, max, percentiles (p50, p95, p99)
 *
 * Gauge — current value that can go up or down
 *   Use for: current state (active orders, queue depth, connection pool size)
 *   Query: orders_active → current number of active orders
 *
 * These metrics are exposed at GET /actuator/metrics and scraped by Prometheus.
 */
@Component
public class OrderMetrics {

    private final Counter ordersPlacedCounter;
    private final Counter ordersCancelledCounter;
    private final Counter orderErrorsCounter;
    private final Timer   orderProcessingTimer;
    private final AtomicInteger activeOrdersGauge;

    public OrderMetrics(MeterRegistry registry) {
        // Counter: total orders placed since service started
        this.ordersPlacedCounter = Counter.builder("orders.placed")
                .description("Total number of orders placed")
                .tag("service", "order-service")
                .register(registry);

        // Counter: total orders cancelled
        this.ordersCancelledCounter = Counter.builder("orders.cancelled")
                .description("Total number of orders cancelled")
                .register(registry);

        // Counter: order placement errors (product unavailable, insufficient stock)
        this.orderErrorsCounter = Counter.builder("orders.errors")
                .description("Total order placement failures")
                .register(registry);

        // Timer: how long placeOrder() takes end-to-end
        this.orderProcessingTimer = Timer.builder("orders.processing.time")
                .description("Time taken to process an order placement")
                .tag("service", "order-service")
                .register(registry);

        // Gauge: current number of active (non-terminal) orders
        // AtomicInteger because Gauge reads the value on every scrape
        this.activeOrdersGauge = new AtomicInteger(0);
        Gauge.builder("orders.active", activeOrdersGauge, AtomicInteger::get)
                .description("Current number of active orders (PENDING + CONFIRMED + SHIPPED)")
                .register(registry);
    }

    public void incrementOrdersPlaced()    { ordersPlacedCounter.increment(); }
    public void incrementOrdersCancelled() { ordersCancelledCounter.increment(); }
    public void incrementOrderErrors()     { orderErrorsCounter.increment(); }
    public Timer orderProcessingTimer()    { return orderProcessingTimer; }
    public void setActiveOrders(int count) { activeOrdersGauge.set(count); }
}
