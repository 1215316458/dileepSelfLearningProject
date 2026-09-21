package com.ecommerce.order_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.ecommerce.order_service.client.ProductClient;
import com.ecommerce.order_service.event.OrderEventPublisher;
import com.ecommerce.order_service.metrics.OrderMetrics;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
class OrderServiceApplicationTests {

    @MockitoBean(name = "restClientProductClient")
    ProductClient productClient;

    @MockitoBean
    OrderEventPublisher eventPublisher;

    @MockitoBean
    OrderMetrics metrics;

    @Test
    void contextLoads() {}
}
