package com.ecommerce.order_service.repository;

import com.ecommerce.order_service.domain.entity.Order;
import com.ecommerce.order_service.domain.entity.OrderItem;
import com.ecommerce.order_service.domain.enums.OrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.ecommerce.order_service.client.ProductClient;
import com.ecommerce.order_service.event.OrderEventPublisher;
import com.ecommerce.order_service.metrics.OrderMetrics;
import io.micrometer.core.instrument.Timer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
class OrderRepositoryTest {

    @Autowired OrderRepository orderRepository;

    @MockitoBean(name = "restClientProductClient")
    ProductClient productClient;

    @MockitoBean
    OrderEventPublisher eventPublisher;

    @MockitoBean
    OrderMetrics metrics;

    private Order savedOrder;

    @BeforeEach
    void setUp() {
        Timer mockTimer = mock(Timer.class);
        when(metrics.orderProcessingTimer()).thenReturn(mockTimer);
        doAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get())
                .when(mockTimer).record(any(Supplier.class));

        Order order = new Order(1L, "123 Main St");
        OrderItem item = new OrderItem(10L, "Laptop", new BigDecimal("999.99"), 2, order);
        order.addItem(item);
        savedOrder = orderRepository.save(order);
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
    }

    @Test
    void findByIdWithItems_loadsItemsEagerly() {
        Optional<Order> found = orderRepository.findByIdWithItems(savedOrder.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getItems()).hasSize(1);
        assertThat(found.get().getItems().get(0).getProductName()).isEqualTo("Laptop");
    }

    @Test
    void findByUserId_returnsOrdersForUser() {
        Order another = new Order(1L, "456 Oak Ave");
        orderRepository.save(another);

        List<Order> orders = orderRepository.findByUserId(1L);
        assertThat(orders).hasSize(2);
    }

    @Test
    void findByStatus_returnsPendingOrders() {
        List<Order> pending = orderRepository.findByStatus(OrderStatus.PENDING);
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getId()).isEqualTo(savedOrder.getId());
    }

    @Test
    void findStuckOrders_returnsOldPendingOrders() {
        // cutoff = now — any PENDING order created before now is "stuck"
        Instant cutoff = Instant.now().plus(1, ChronoUnit.MINUTES);
        List<Order> stuck = orderRepository.findStuckOrders(cutoff);
        assertThat(stuck).hasSize(1);
    }

    @Test
    void findStuckOrders_excludesRecentOrders() {
        // cutoff = 1 hour ago — our order was just created, so it's not stuck
        Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);
        List<Order> stuck = orderRepository.findStuckOrders(cutoff);
        assertThat(stuck).isEmpty();
    }

    @Test
    void totalAmount_calculatedCorrectly() {
        Order found = orderRepository.findByIdWithItems(savedOrder.getId()).orElseThrow();
        // 999.99 * 2 = 1999.98
        assertThat(found.getTotalAmount()).isEqualByComparingTo("1999.98");
    }
}
