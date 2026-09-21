package com.ecommerce.order_service.dto;

import java.math.BigDecimal;

// DTO representing the product data we need from product-service
// Only the fields we actually use — not the full product entity
public record ProductResponse(
    Long       id,
    String     name,
    BigDecimal price,
    Integer    stock,
    String     category
) {}
