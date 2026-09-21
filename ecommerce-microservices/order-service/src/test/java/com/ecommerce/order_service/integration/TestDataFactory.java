package com.ecommerce.order_service.integration;

import com.ecommerce.order_service.dto.PlaceOrderRequest;

import java.util.List;

/**
 * Centralises test data creation so tests stay readable.
 * A factory like this prevents duplication and makes intent clear.
 */
public class TestDataFactory {

    public static PlaceOrderRequest validOrderRequest() {
        return new PlaceOrderRequest(
                1L,
                "123 Test Street, Test City",
                List.of(new PlaceOrderRequest.OrderItemRequest(10L, 2))
        );
    }

    public static PlaceOrderRequest orderRequestForUser(Long userId) {
        return new PlaceOrderRequest(
                userId,
                "456 Integration Ave",
                List.of(new PlaceOrderRequest.OrderItemRequest(20L, 1))
        );
    }

    public static PlaceOrderRequest multiItemOrderRequest() {
        return new PlaceOrderRequest(
                1L,
                "789 Multi Item Road",
                List.of(
                        new PlaceOrderRequest.OrderItemRequest(10L, 1),
                        new PlaceOrderRequest.OrderItemRequest(11L, 3)
                )
        );
    }
}
