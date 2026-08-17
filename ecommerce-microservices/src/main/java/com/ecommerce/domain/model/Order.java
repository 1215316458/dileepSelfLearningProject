package com.ecommerce.domain.model;

import com.ecommerce.domain.enums.OrderStatus;
import com.ecommerce.domain.exception.InvalidOrderStateException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Order extends BaseEntity<Long> {

    private Long userId;
    private List<CartItem> items;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String shippingAddress;

    // private constructor — only the Builder can create an Order
    private Order() {}

    // Validates and moves to the next status, throws if the transition is illegal
    public void transitionTo(OrderStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new InvalidOrderStateException(
                "Cannot transition from " + this.status + " to " + newStatus
                + ". Allowed: " + this.status.allowedTransitions()
            );
        }
        this.status = newStatus;
        setUpdatedAt(LocalDateTime.now());
    }

    // Recalculates total by summing all CartItem subtotals
    private BigDecimal calculateTotal() {
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getUserId() { return userId; }
    public List<CartItem> getItems() { return Collections.unmodifiableList(items); }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public String getShippingAddress() { return shippingAddress; }

    @Override
    public String toString() {
        return new StringBuilder("Order{")
                .append("id=").append(getIdentity())
                .append(", userId=").append(userId)
                .append(", status=").append(status)
                .append(", total=").append(totalAmount)
                .append(", items=").append(items.size())
                .append(", shippingAddress=").append(shippingAddress)
                .append('}')
                .toString();
    }

    // -------------------------
    // Builder — Order has many fields, some optional. Builder avoids telescoping constructors.
    // -------------------------
    public static class Builder {

        private Long id;
        private Long userId;
        private final List<CartItem> items = new ArrayList<>();
        private OrderStatus status = OrderStatus.PENDING; // sensible default
        private String shippingAddress;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder addItem(CartItem item) {
            this.items.add(item);
            return this;
        }

        public Builder status(OrderStatus status) {
            this.status = status;
            return this;
        }

        public Builder shippingAddress(String shippingAddress) {
            this.shippingAddress = shippingAddress;
            return this;
        }

        public Order build() {
            if (userId == null) throw new IllegalStateException("userId is required");
            if (items.isEmpty()) throw new IllegalStateException("Order must have at least one item");

            Order order = new Order();
            order.setIdentity(id);
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            order.userId = userId;
            order.items = new ArrayList<>(items); // defensive copy
            order.status = status;
            order.shippingAddress = shippingAddress;
            order.totalAmount = order.calculateTotal();
            return order;
        }
    }
}
