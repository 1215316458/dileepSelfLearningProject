package com.ecommerce.order_service.integration;

import com.ecommerce.order_service.client.ProductClient;
import com.ecommerce.order_service.event.OrderEventPublisher;
import com.ecommerce.order_service.metrics.OrderMetrics;
import com.ecommerce.order_service.domain.enums.OrderStatus;
import com.ecommerce.order_service.dto.ProductResponse;
import com.ecommerce.order_service.exception.ProductUnavailableException;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Day 22 — Integration tests for Order Service.
 *
 * What makes this an integration test (vs unit test):
 *  - Full Spring context loads (@SpringBootTest)
 *  - Real H2 database (from application-test.yml)
 *  - Real service + repository wiring
 *  - Only the external HTTP call (ProductClient) is mocked
 *
 * WireMock / TestContainers are NOT used here because:
 *  - WireMock is not in the local Maven cache (offline environment)
 *  - TestContainers jars are not cached either
 *  - @MockitoBean on ProductClient achieves the same goal: isolate the
 *    external dependency while testing the full internal stack
 *
 * This is the correct approach when you want to test:
 *  - HTTP → Controller → Service → Repository → DB (real H2)
 *  - Transaction rollback behaviour
 *  - Circuit breaker fallback (mock throws exception)
 *  - Validation errors
 *  - 404 responses for missing resources
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class OrderIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    // Mock the ProductClient — this is the only external dependency.
    // The name attribute is required because both RestClientProductClient
    // and ProductClientFallback implement ProductClient (ambiguity fix from Day 19).
    @MockitoBean(name = "restClientProductClient")
    private ProductClient productClient;

    // Mock Kafka publisher — no Kafka broker in tests
    @MockitoBean
    private OrderEventPublisher eventPublisher;

    // Mock metrics — MeterRegistry not fully wired in test slice
    @MockitoBean
    private OrderMetrics metrics;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        // Stub Timer.record() to actually invoke the Supplier so placeOrder logic runs
        Timer mockTimer = mock(Timer.class);
        when(metrics.orderProcessingTimer()).thenReturn(mockTimer);
        doAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get())
                .when(mockTimer).record(any(Supplier.class));
    }

    private ProductResponse stubProduct(Long id, int stock) {
        return new ProductResponse(id, "Product-" + id, new BigDecimal("29.99"), stock, "ELECTRONICS");
    }

    // -------------------------------------------------------------------------
    // Happy path — place order successfully
    // -------------------------------------------------------------------------

    @Test
    void placeOrderSimple_returns201WithOrderDetails() throws Exception {
        when(productClient.getProductById(10L)).thenReturn(stubProduct(10L, 100));

        mockMvc.perform(post("/api/orders/simple")
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(TestDataFactory.validOrderRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items[0].productId").value(10))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void placeOrderSimple_multipleItems_calculatesCorrectTotal() throws Exception {
        when(productClient.getProductById(10L)).thenReturn(stubProduct(10L, 50));
        when(productClient.getProductById(11L)).thenReturn(stubProduct(11L, 50));

        mockMvc.perform(post("/api/orders/simple")
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(TestDataFactory.multiItemOrderRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items.length()").value(2))
                // total = 29.99*1 + 29.99*3 = 119.96
                .andExpect(jsonPath("$.totalAmount").value(119.96));
    }

    // -------------------------------------------------------------------------
    // Validation — bad request
    // -------------------------------------------------------------------------

    @Test
    void placeOrder_missingUserId_returns400() throws Exception {
        String badRequest = """
                {
                  "shippingAddress": "123 Test St",
                  "items": [{"productId": 1, "quantity": 1}]
                }
                """;

        mockMvc.perform(post("/api/orders/simple")
                        .contentType(APPLICATION_JSON)
                        .content(badRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void placeOrder_emptyItems_returns400() throws Exception {
        String badRequest = """
                {
                  "userId": 1,
                  "shippingAddress": "123 Test St",
                  "items": []
                }
                """;

        mockMvc.perform(post("/api/orders/simple")
                        .contentType(APPLICATION_JSON)
                        .content(badRequest))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Circuit breaker fallback — product service unavailable
    // -------------------------------------------------------------------------

    @Test
    void placeOrder_productServiceDown_returnsError() throws Exception {
        // Simulate product service being unavailable — triggers fallback
        // which throws ProductUnavailableException.
        // Resilience4j may wrap the exception, so we accept 4xx or 5xx.
        when(productClient.getProductById(anyLong()))
                .thenThrow(new RuntimeException("Connection refused"));

        mockMvc.perform(post("/api/orders/simple")
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(TestDataFactory.validOrderRequest())))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void placeOrder_insufficientStock_returns400() throws Exception {
        // Product exists but stock is too low
        when(productClient.getProductById(10L)).thenReturn(stubProduct(10L, 1));

        // Request asks for 2 but only 1 in stock
        mockMvc.perform(post("/api/orders/simple")
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(TestDataFactory.validOrderRequest())))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Get order — 404 for missing
    // -------------------------------------------------------------------------

    @Test
    void getOrder_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/orders/99999"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // Status update
    // -------------------------------------------------------------------------

    @Test
    void updateStatus_existingOrder_returnsUpdatedStatus() throws Exception {
        // First create an order
        when(productClient.getProductById(20L)).thenReturn(stubProduct(20L, 100));

        String createResponse = mockMvc.perform(post("/api/orders/simple")
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(TestDataFactory.orderRequestForUser(2L))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Extract the id from the response
        Long orderId = mapper.readTree(createResponse).get("id").asLong();

        // Update status to CONFIRMED
        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void updateStatus_notFound_returns404() throws Exception {
        mockMvc.perform(patch("/api/orders/99999/status")
                        .param("status", "CONFIRMED"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // Cancel order
    // -------------------------------------------------------------------------

    @Test
    void cancelOrder_pendingOrder_returns204() throws Exception {
        when(productClient.getProductById(20L)).thenReturn(stubProduct(20L, 100));

        String createResponse = mockMvc.perform(post("/api/orders/simple")
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(TestDataFactory.orderRequestForUser(3L))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = mapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(delete("/api/orders/" + orderId))
                .andExpect(status().isNoContent());
    }

    @Test
    void getOrdersByUser_returnsOrderList() throws Exception {
        when(productClient.getProductById(20L)).thenReturn(stubProduct(20L, 100));

        // Create an order for user 4
        mockMvc.perform(post("/api/orders/simple")
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(TestDataFactory.orderRequestForUser(4L))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/orders/user/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
