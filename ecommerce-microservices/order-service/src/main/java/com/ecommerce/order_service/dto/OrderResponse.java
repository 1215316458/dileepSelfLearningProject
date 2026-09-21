package com.ecommerce.order_service.dto;

import com.ecommerce.order_service.domain.entity.Order;
import com.ecommerce.order_service.domain.entity.OrderItem;
import com.ecommerce.order_service.domain.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
    Long            id,
    Long            userId,
    OrderStatus     status,
    BigDecimal      totalAmount,
    String          shippingAddress,
    List<ItemResponse> items,
    Instant         createdAt
) {
    public record ItemResponse(
        Long       productId,
        String     productName,
        BigDecimal unitPrice,
        Integer    quantity,
        BigDecimal subtotal
    ) {
        static ItemResponse from(OrderItem item) {
            return new ItemResponse(item.getProductId(), item.getProductName(),
                    item.getUnitPrice(), item.getQuantity(), item.getSubtotal());
        }
    }

    public static OrderResponse from(Order order) {
        List<ItemResponse> items = order.getItems().stream()
                .map(ItemResponse::from)
                .toList();
        return new OrderResponse(order.getId(), order.getUserId(), order.getStatus(),
                order.getTotalAmount(), order.getShippingAddress(), items, order.getCreatedAt());
    }
}
