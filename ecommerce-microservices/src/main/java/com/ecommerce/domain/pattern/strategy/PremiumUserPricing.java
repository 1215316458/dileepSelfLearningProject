package com.ecommerce.domain.pattern.strategy;

import java.math.BigDecimal;

import com.ecommerce.domain.model.Product;
public class PremiumUserPricing implements PricingStrategy {
    @Override
    public BigDecimal calculatePrice(Product product, int quantity) {
        BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        return total.multiply(new BigDecimal("0.80")).setScale(2, java.math.RoundingMode.HALF_UP); // 20% off
    }
}
