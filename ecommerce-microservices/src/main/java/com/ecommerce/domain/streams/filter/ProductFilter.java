package com.ecommerce.domain.streams.filter;

import java.util.function.Predicate;

import com.ecommerce.domain.enums.Category;
import com.ecommerce.domain.model.Product;
import java.math.BigDecimal;

public class ProductFilter {

    public static Predicate<Product> byCategory(Category category) {
        return p -> p.getCategory() == category;
    }

    public static Predicate<Product> byPriceRange(BigDecimal min, BigDecimal max) {
        return p -> p.getPrice().compareTo(min) >= 0 && p.getPrice().compareTo(max) <= 0;
    }

    public static Predicate<Product> byInStock() {
        return p -> p.getStock() > 0;
    }

    public static Predicate<Product> byNameContains(String searchTerm) {
        return p -> p.getName().toLowerCase().contains(searchTerm.toLowerCase());
    }

}
