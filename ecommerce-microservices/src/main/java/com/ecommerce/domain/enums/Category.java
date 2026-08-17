package com.ecommerce.domain.enums;

import java.math.BigDecimal;
import java.util.Optional;

public enum Category {

    ELECTRONICS("Electronics", new BigDecimal("0.18")),
    CLOTHING("Clothing", new BigDecimal("0.12")),
    BOOKS("Books", new BigDecimal("0.05")),
    FOOD("Food", new BigDecimal("0.00")),
    SPORTS("Sports", new BigDecimal("0.10"));

    private final String displayName;
    // BigDecimal instead of double — floating point can't represent 0.1 exactly, money needs precision
    private final BigDecimal taxRate;

    Category(String displayName, BigDecimal taxRate) {
        this.displayName = displayName;
        this.taxRate = taxRate;
    }

    public String getDisplayName() { return displayName; }
    public BigDecimal getTaxRate() { return taxRate; }

    // Optional — forces the caller to handle the "not found" case instead of getting a NullPointerException
    public static Optional<Category> fromString(String value) {
        if (value == null) return Optional.empty();
        for (Category category : values()) {
            if (category.displayName.equalsIgnoreCase(value) || category.name().equalsIgnoreCase(value)) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }
}
