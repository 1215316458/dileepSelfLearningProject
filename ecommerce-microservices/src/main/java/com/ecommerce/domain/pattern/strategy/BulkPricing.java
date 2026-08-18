package com.ecommerce.domain.pattern.strategy;

import java.math.BigDecimal;

import com.ecommerce.domain.model.Product;

public class BulkPricing implements PricingStrategy {

    @Override
    public BigDecimal calculatePrice(Product product, int quantity) {
        // multiply total first, then apply discount — not the other way around
        BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        if (quantity >= 10) {
            total = total.multiply(new BigDecimal("0.90")); // 10% discount
        }
        return total.setScale(2, java.math.RoundingMode.HALF_UP);
    }

}
