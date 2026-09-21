package com.ecommerce.order_service.service;

import com.ecommerce.order_service.domain.enums.OrderStatus;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.dto.PlaceOrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// SagaOrchestrator — coordinates a multi-step distributed transaction
//
// Saga pattern: instead of a single distributed transaction (which requires 2PC),
// each step is a local transaction. If a step fails, compensation steps undo
// the previous steps.
//
// Our saga:
//   Step 1: Create order (PENDING)
//   Step 2: Reserve stock in product-service (Feign call)
//   Step 3: Process payment (simulated)
//   Step 4: Confirm order (CONFIRMED)
//
// Compensation:
//   If step 3 fails → release stock (compensate step 2) → cancel order (compensate step 1)
//   If step 2 fails → cancel order (compensate step 1)
@Service
public class SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);

    private final OrderService orderService;

    public SagaOrchestrator(OrderService orderService) {
        this.orderService = orderService;
    }

    // execute — runs the full saga with compensation on failure
    // Each step is a separate local transaction (not one big distributed transaction)
    public OrderResponse execute(PlaceOrderRequest req) {
        OrderResponse order = null;

        // Step 1: Create order
        try {
            order = orderService.placeOrder(req);
            log.info("[SAGA] Step 1 complete — order created id={}", order.id());
        } catch (Exception e) {
            log.error("[SAGA] Step 1 failed — order creation failed: {}", e.getMessage());
            throw e;   // nothing to compensate yet
        }

        // Step 2: Reserve stock
        try {
            reserveStock(order);
            log.info("[SAGA] Step 2 complete — stock reserved for order id={}", order.id());
        } catch (Exception e) {
            log.error("[SAGA] Step 2 failed — compensating: cancelling order id={}", order.id());
            compensateCancelOrder(order.id());   // compensation for step 1
            throw new RuntimeException("Stock reservation failed, order cancelled", e);
        }

        // Step 3: Process payment
        try {
            processPayment(order);
            log.info("[SAGA] Step 3 complete — payment processed for order id={}", order.id());
        } catch (Exception e) {
            log.error("[SAGA] Step 3 failed — compensating: releasing stock + cancelling order id={}", order.id());
            compensateReleaseStock(order);       // compensation for step 2
            compensateCancelOrder(order.id());   // compensation for step 1
            throw new RuntimeException("Payment failed, order cancelled and stock released", e);
        }

        // Step 4: Confirm order
        OrderResponse confirmed = orderService.updateStatus(order.id(), OrderStatus.CONFIRMED);
        log.info("[SAGA] Step 4 complete — order confirmed id={}", confirmed.id());
        return confirmed;
    }

    // --- Saga steps (each would be a Feign call in a real system) ---

    private void reserveStock(OrderResponse order) {
        // In production: POST /api/products/reserve { orderId, items }
        // For now: validate stock is available via circuit-breaker-protected Feign call
        order.items().forEach(item -> {
            boolean available = orderService.checkStock(item.productId(), item.quantity());
            if (!available) {
                throw new IllegalArgumentException(
                    "Stock reservation failed for product " + item.productId());
            }
        });
    }

    private void processPayment(OrderResponse order) {
        // In production: POST /api/payments { orderId, amount, userId }
        // Simulated: always succeeds (payment-service is Day 25+)
        log.info("[SAGA] Processing payment of {} for order id={}", order.totalAmount(), order.id());
    }

    // --- Compensation steps ---

    private void compensateCancelOrder(Long orderId) {
        try {
            orderService.cancelOrder(orderId);
            log.info("[SAGA] Compensation: order {} cancelled", orderId);
        } catch (Exception e) {
            // compensation failed — needs manual intervention / dead letter queue
            log.error("[SAGA] COMPENSATION FAILED for order {}: {}", orderId, e.getMessage());
        }
    }

    private void compensateReleaseStock(OrderResponse order) {
        // In production: POST /api/products/release { orderId, items }
        log.info("[SAGA] Compensation: releasing stock for order id={}", order.id());
    }
}
