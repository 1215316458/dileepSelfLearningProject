package com.ecommerce.domain.model;

import java.math.BigDecimal;

public class CartItem {

    // fields are final — CartItem is immutable by design.
    // If you need a different quantity, create a new CartItem instead of mutating this one.
    // Immutability prevents accidental state changes when CartItem is shared across collections.
    private final Product product;
    private final int quantity;

    public CartItem(Product product, int quantity) {
        if (product == null) throw new IllegalArgumentException("Product cannot be null");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        this.product = product;
        this.quantity = quantity;
    }

    // BigDecimal.multiply — precise arithmetic, no floating point rounding errors
    public BigDecimal getSubtotal() {
        return product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }

    @Override
    public String toString() {
        return new StringBuilder("CartItem{")
                .append("product=").append(product.getName())
                .append(", quantity=").append(quantity)
                .append(", subtotal=").append(getSubtotal())
                .append('}')
                .toString();
    }
}
