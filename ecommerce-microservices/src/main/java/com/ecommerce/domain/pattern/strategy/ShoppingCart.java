package com.ecommerce.domain.pattern.strategy;

import com.ecommerce.domain.model.CartItem;
import com.ecommerce.domain.model.Product;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// Context — holds a reference to a PricingStrategy and delegates price calculation to it.
// ShoppingCart doesn't know or care which strategy is active — it just calls calculatePrice().
// Map<productId, CartItem> — keyed by productId so addItem on same product merges quantity
// instead of creating duplicate entries.
public class ShoppingCart {

    private PricingStrategy pricingStrategy;

    // LinkedHashMap — preserves insertion order so cart items display in the order they were added
    private final Map<Long, CartItem> items = new LinkedHashMap<>();

    public ShoppingCart(PricingStrategy pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }

    // swap strategy at runtime — e.g. user upgrades to premium mid-session
    public void setPricingStrategy(PricingStrategy pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }

    public void addItem(Product product, int quantity) {
        // if product already in cart, merge quantity into a new CartItem
        // CartItem is immutable (final fields) so we replace rather than mutate
        items.merge(product.getIdentity(),
                new CartItem(product, quantity),
                (existing, incoming) -> new CartItem(product, existing.getQuantity() + incoming.getQuantity()));
    }

    public void removeItem(Long productId) {
        items.remove(productId);
    }

    public void updateQuantity(Long productId, int newQuantity) {
        if (newQuantity <= 0) {
            removeItem(productId); // quantity 0 or negative means remove
            return;
        }
        CartItem existing = items.get(productId);
        if (existing != null) {
            items.put(productId, new CartItem(existing.getProduct(), newQuantity));
        }
    }

    // unmodifiableMap — callers can iterate but cannot mutate the cart directly
    public Map<Long, CartItem> getItems() {
        return Collections.unmodifiableMap(items);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void clear() {
        items.clear();
    }

    public BigDecimal getTotalAmount() {
        return items.values().stream()
                .map(item -> pricingStrategy.calculatePrice(item.getProduct(), item.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
