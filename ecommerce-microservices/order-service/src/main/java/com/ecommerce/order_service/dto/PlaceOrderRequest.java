package com.ecommerce.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record PlaceOrderRequest(
    @NotNull                    Long userId,
    @NotBlank                   String shippingAddress,
    @NotEmpty @Valid            List<OrderItemRequest> items
) {
    public record OrderItemRequest(
        @NotNull              Long productId,
        @Positive             int  quantity
    ) {}
}
