package com.ecommerce.order_service.controller;

import com.ecommerce.order_service.domain.enums.OrderStatus;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.dto.PlaceOrderRequest;
import com.ecommerce.order_service.service.OrderService;
import com.ecommerce.order_service.service.SagaOrchestrator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService     orderService;
    private final SagaOrchestrator sagaOrchestrator;

    public OrderController(OrderService orderService, SagaOrchestrator sagaOrchestrator) {
        this.orderService     = orderService;
        this.sagaOrchestrator = sagaOrchestrator;
    }

    // POST /api/orders — place order via full saga (validate → reserve → pay → confirm)
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sagaOrchestrator.execute(req));
    }

    // POST /api/orders/simple — place order without saga (Day 19 basic version)
    @PostMapping("/simple")
    public ResponseEntity<OrderResponse> placeOrderSimple(@Valid @RequestBody PlaceOrderRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUser(userId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
        @PathVariable Long id,
        @RequestParam OrderStatus status
    ) {
        return ResponseEntity.ok(orderService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }
}
