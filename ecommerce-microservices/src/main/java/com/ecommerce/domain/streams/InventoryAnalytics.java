package com.ecommerce.domain.streams;

import com.ecommerce.domain.model.Order;
import com.ecommerce.domain.model.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class InventoryAnalytics {

    // Products with stock below threshold, sorted by stock ascending (most urgent first)
    public static List<Product> lowStockAlerts(List<Product> products, int threshold) {
        return products.stream()
                .filter(p -> p.getStock() > 0 && p.getStock() <= threshold)
                .sorted(Product.BY_STOCK)
                .collect(Collectors.toList());
    }

    // Total inventory value = sum of (price * stock) for all products
    // map each product to its value, reduce to a single sum
    public static BigDecimal totalInventoryValue(List<Product> products) {
        return products.stream()
                .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Dead stock: products with zero orders placed after the cutoff date
    // collect productIds ordered after cutoff, filter products not in that set
    public static List<Product> deadStock(List<Product> products, List<Order> orders, LocalDateTime since) {
        Set<Long> recentlyOrderedIds = orders.stream()
                .filter(o -> o.getCreatedAt().isAfter(since))
                .flatMap(o -> o.getItems().stream())
                .map(item -> item.getProduct().getIdentity())
                .collect(Collectors.toSet());

        return products.stream()
                .filter(p -> !recentlyOrderedIds.contains(p.getIdentity()))
                .collect(Collectors.toList());
    }

    // findCheapestInCategory — Optional forces caller to handle "no products found" case
    public static Optional<Product> findCheapestInCategory(List<Product> products, com.ecommerce.domain.enums.Category category) {
        return products.stream()
                .filter(p -> p.getCategory() == category)
                .min(Product.BY_PRICE);
    }
}
