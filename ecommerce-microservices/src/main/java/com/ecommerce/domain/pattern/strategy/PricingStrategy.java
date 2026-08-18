package com.ecommerce.domain.pattern.strategy;

import java.math.BigDecimal;

import com.ecommerce.domain.model.Product;

public interface PricingStrategy {
    BigDecimal calculatePrice(Product product, int quantity);
}
