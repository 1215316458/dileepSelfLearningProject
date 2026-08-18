package com.ecommerce.domain.pattern.strategy;

import com.ecommerce.domain.model.CartItem;
import com.ecommerce.domain.model.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Context — holds a reference to a PricingStrategy and delegates price calculation to it.
// ShoppingCart doesn't know or care which strategy is active — it just calls calculatePrice().
public class ShoppingCart {

    private PricingStrategy pricingStrategy;
    private final List<CartItem> items = new ArrayList<>();

    public ShoppingCart(PricingStrategy pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }

    // swap strategy at runtime — e.g. user upgrades to premium mid-session
    public void setPricingStrategy(PricingStrategy pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }

    public void addItem(Product product, int quantity) {
        items.add(new CartItem(product, quantity));
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public BigDecimal calculateTotal() {
        return items.stream()
                .map(item -> pricingStrategy.calculatePrice(item.getProduct(), item.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
