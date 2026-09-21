package com.ecommerce.order_service.service;

import com.ecommerce.order_service.event.OrderEventPublisher;
import com.ecommerce.order_service.event.OrderPlacedEvent;
import com.ecommerce.order_service.metrics.OrderMetrics;
import com.ecommerce.order_service.client.ProductClient;
import com.ecommerce.order_service.domain.entity.Order;
import com.ecommerce.order_service.domain.entity.OrderItem;
import com.ecommerce.order_service.domain.enums.OrderStatus;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.dto.PlaceOrderRequest;
import com.ecommerce.order_service.dto.ProductResponse;
import com.ecommerce.order_service.exception.OrderNotFoundException;
import com.ecommerce.order_service.exception.ProductUnavailableException;
import com.ecommerce.order_service.repository.OrderRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductClient   productClient;
    private final AuditLogService auditLogService;
    private final OrderEventPublisher eventPublisher;
    private final OrderMetrics metrics;

    public OrderService(OrderRepository orderRepository,
                        ProductClient productClient,
                        AuditLogService auditLogService,
                        OrderEventPublisher eventPublisher,
                        OrderMetrics metrics) {
        this.orderRepository = orderRepository;
        this.productClient   = productClient;
        this.auditLogService = auditLogService;
        this.eventPublisher  = eventPublisher;
        this.metrics         = metrics;
    }

    // -------------------------------------------------------------------------
    // Day 19 — Feign + Resilience4j
    // -------------------------------------------------------------------------

    // @CircuitBreaker — wraps the Feign call; if product-service fails repeatedly,
    //   the circuit opens and fallback is called immediately (no waiting for timeout)
    // @Retry — retries up to 3 times with exponential backoff before giving up
    // @Bulkhead — limits concurrent calls to product-service to 10
    // Order of decoration: Bulkhead → TimeLimiter → CircuitBreaker → Retry → method
    // @CircuitBreaker — if product-service fails repeatedly, circuit opens and
    //   fallback fires immediately without waiting for timeout
    // @Retry — retries up to 3x with exponential backoff before giving up
    // @Bulkhead — limits concurrent calls to product-service to 10
    @CircuitBreaker(name = "product-service", fallbackMethod = "getProductFallback")
    @Retry(name = "product-service")
    @Bulkhead(name = "product-service")
    public ProductResponse getProduct(Long productId) {
        return productClient.getProductById(productId);
    }

    // Fallback — called when circuit is OPEN or all retries exhausted
    // Must have same return type, same params, plus Throwable as last param
    public ProductResponse getProductFallback(Long productId, Throwable ex) {
        log.warn("Product service unavailable for productId={}, reason={}", productId, ex.getMessage());
        throw new ProductUnavailableException(productId);
    }

    // -------------------------------------------------------------------------
    // Day 20 — @Transactional + Saga
    // -------------------------------------------------------------------------

    // @Transactional — wraps the entire placeOrder in one DB transaction
    // If any step throws, the whole transaction rolls back (no partial order)
    // Saga steps: validate products → create order → (async) send confirmation
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest req) {
        return metrics.orderProcessingTimer().record(() -> {
            Order order = new Order(req.userId(), req.shippingAddress());

            for (PlaceOrderRequest.OrderItemRequest itemReq : req.items()) {
                ProductResponse product = getProduct(itemReq.productId());

                if (product.stock() < itemReq.quantity()) {
                    metrics.incrementOrderErrors();
                    throw new IllegalArgumentException(
                        "Insufficient stock for product " + itemReq.productId() +
                        ": requested=" + itemReq.quantity() + ", available=" + product.stock());
                }

                OrderItem item = new OrderItem(
                    product.id(), product.name(), product.price(), itemReq.quantity(), order);
                order.addItem(item);
            }

            Order saved = orderRepository.save(order);
            auditLogService.logOrderCreated(saved.getId(), saved.getUserId());
            eventPublisher.publishOrderPlaced(
                    OrderPlacedEvent.of(saved.getId(), saved.getUserId(),
                            saved.getTotalAmount(), saved.getShippingAddress()));
            sendConfirmationAsync(saved.getId(), saved.getUserId());

            metrics.incrementOrdersPlaced();
            return OrderResponse.from(saved);
        });
    }

    // REPEATABLE_READ — prevents phantom reads during stock check
    // If two transactions read stock simultaneously, neither sees the other's uncommitted changes
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public boolean checkStock(Long productId, int required) {
        ProductResponse product = getProduct(productId);
        return product.stock() >= required;
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        return OrderResponse.from(
            orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new OrderNotFoundException(id))
        );
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId, Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(OrderResponse::from).toList();
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        order.setStatus(status);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public void cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Cannot cancel order in status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        auditLogService.logOrderCancelled(id, order.getUserId());
        metrics.incrementOrdersCancelled();
    }

    // -------------------------------------------------------------------------
    // Day 20 — @Async
    // -------------------------------------------------------------------------

    // @Async — runs in a separate thread from the "orderExecutor" pool (configured in AsyncConfig)
    // Returns CompletableFuture so callers can chain or ignore the result
    // The @Transactional on placeOrder has already committed before this runs
    @Async("orderExecutor")
    public CompletableFuture<Void> sendConfirmationAsync(Long orderId, Long userId) {
        log.info("[ASYNC] Sending order confirmation for orderId={}, userId={}", orderId, userId);
        // In production: call notification-service or send email via SES
        // Simulated delay
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        log.info("[ASYNC] Confirmation sent for orderId={}", orderId);
        return CompletableFuture.completedFuture(null);
    }

    // -------------------------------------------------------------------------
    // Day 20 — @Scheduled
    // -------------------------------------------------------------------------

    // Detect orders stuck in PENDING for more than 30 minutes
    // fixedDelay — waits 5 minutes AFTER the previous execution finishes before running again
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    @Transactional
    public void detectStuckOrders() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.MINUTES);
        List<Order> stuck = orderRepository.findStuckOrders(cutoff);
        if (!stuck.isEmpty()) {
            log.warn("[SCHEDULER] Found {} stuck PENDING orders older than 30 minutes", stuck.size());
            stuck.forEach(o -> {
                o.setStatus(OrderStatus.CANCELLED);
                log.warn("[SCHEDULER] Auto-cancelling stuck order id={}", o.getId());
            });
            orderRepository.saveAll(stuck);
        }
    }

    // Nightly archival — runs at 2:00 AM every day
    // cron = "second minute hour day-of-month month day-of-week"
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional(readOnly = true)
    public void nightlyReport() {
        long total = orderRepository.count();
        long delivered = orderRepository.findByStatus(OrderStatus.DELIVERED).size();
        log.info("[SCHEDULER] Nightly report — total orders={}, delivered={}", total, delivered);
    }
}
