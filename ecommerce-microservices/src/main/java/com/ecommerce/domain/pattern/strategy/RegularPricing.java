package com.ecommerce.domain.pattern.strategy;

import java.math.BigDecimal;

import com.ecommerce.domain.model.Product;
public class RegularPricing implements PricingStrategy {

    @Override
    public BigDecimal calculatePrice(Product product, int quantity) {
        return product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }
}
