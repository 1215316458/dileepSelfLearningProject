package com.ecommerce.order_service.client;

import com.ecommerce.order_service.dto.ProductResponse;

import java.math.BigDecimal;

// Fallback — returned when the circuit is OPEN or product-service is unreachable.
// Not a Spring bean — instantiated directly in OrderService.getProductFallback().
// In production: return cached data from Redis (Day 27).
public class ProductClientFallback implements ProductClient {

    @Override
    public ProductResponse getProductById(Long id) {
        return new ProductResponse(id, "Product unavailable", BigDecimal.ZERO, 0, "UNKNOWN");
    }
}
