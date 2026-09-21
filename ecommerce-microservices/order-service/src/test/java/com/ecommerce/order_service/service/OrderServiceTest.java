package com.ecommerce.order_service.service;

import com.ecommerce.order_service.client.ProductClient;
import com.ecommerce.order_service.event.OrderEventPublisher;
import com.ecommerce.order_service.metrics.OrderMetrics;
import com.ecommerce.order_service.domain.enums.OrderStatus;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.dto.PlaceOrderRequest;
import com.ecommerce.order_service.dto.ProductResponse;
import com.ecommerce.order_service.exception.OrderNotFoundException;
import com.ecommerce.order_service.repository.OrderRepository;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// @SpringBootTest with WebEnvironment.NONE — full context, no HTTP server
// Mocks ProductClient so tests don't need a real product-service running
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
class OrderServiceTest {

    @Autowired OrderService    orderService;
    @Autowired OrderRepository orderRepository;

    // Mock by name — two beans implement ProductClient (restClientProductClient + productClientFallback)
    // We replace the primary implementation so Resilience4j calls the mock
    @MockitoBean(name = "restClientProductClient")
    ProductClient productClient;

    @MockitoBean
    OrderEventPublisher eventPublisher;

    @MockitoBean
    OrderMetrics metrics;

    @BeforeEach
    void setUpMetrics() {
        Timer mockTimer = mock(Timer.class);
        when(metrics.orderProcessingTimer()).thenReturn(mockTimer);
        doAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get())
                .when(mockTimer).record(any(Supplier.class));
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
    }

    private PlaceOrderRequest buildRequest(long productId, int qty) {
        return new PlaceOrderRequest(1L, "123 Main St",
                List.of(new PlaceOrderRequest.OrderItemRequest(productId, qty)));
    }

    private void mockProduct(long id, int stock) {
        when(productClient.getProductById(id))
            .thenReturn(new ProductResponse(id, "Test Product", new BigDecimal("50.00"), stock, "ELECTRONICS"));
    }

    @Test
    void placeOrder_savesOrderWithItems() {
        mockProduct(1L, 10);

        OrderResponse response = orderService.placeOrder(buildRequest(1L, 2));

        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.items()).hasSize(1);
        assertThat(response.totalAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void placeOrder_insufficientStock_throwsException() {
        mockProduct(1L, 1);  // only 1 in stock

        assertThatThrownBy(() -> orderService.placeOrder(buildRequest(1L, 5)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Insufficient stock");
    }

    @Test
    void getOrder_notFound_throwsOrderNotFoundException() {
        assertThatThrownBy(() -> orderService.getOrder(999L))
            .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getOrdersByUser_returnsAllUserOrders() {
        mockProduct(1L, 10);
        orderService.placeOrder(buildRequest(1L, 1));
        orderService.placeOrder(buildRequest(1L, 2));

        List<OrderResponse> orders = orderService.getOrdersByUser(1L);
        assertThat(orders).hasSize(2);
    }

    @Test
    void updateStatus_changesOrderStatus() {
        mockProduct(1L, 10);
        OrderResponse created = orderService.placeOrder(buildRequest(1L, 1));

        OrderResponse updated = orderService.updateStatus(created.id(), OrderStatus.CONFIRMED);
        assertThat(updated.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void cancelOrder_pendingOrder_succeeds() {
        mockProduct(1L, 10);
        OrderResponse created = orderService.placeOrder(buildRequest(1L, 1));

        orderService.cancelOrder(created.id());

        OrderResponse cancelled = orderService.getOrder(created.id());
        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrder_shippedOrder_throwsException() {
        mockProduct(1L, 10);
        OrderResponse created = orderService.placeOrder(buildRequest(1L, 1));
        orderService.updateStatus(created.id(), OrderStatus.SHIPPED);

        assertThatThrownBy(() -> orderService.cancelOrder(created.id()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot cancel");
    }
}
