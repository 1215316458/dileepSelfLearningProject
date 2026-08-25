package com.ecommerce.domain.streams;

import com.ecommerce.domain.enums.Category;
import com.ecommerce.domain.model.Order;
import com.ecommerce.domain.model.Product;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ProductAnalytics {

    // Top N products by total quantity sold across all orders
    // flatMap: Order -> CartItem (flatten nested lists into one stream)
    // groupingBy productId, summingInt quantity -> Map<Long, Integer>
    // sort by value descending, take N
    public static List<Map.Entry<Long, Integer>> topNByQuantitySold(List<Order> orders, int n) {
        return orders.stream()
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getIdentity(),
                        Collectors.summingInt(item -> item.getQuantity())
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(n)
                .collect(Collectors.toList());
    }

    // Products that appear in zero orders
    // collect all ordered productIds into a Set, then filter products not in that set
    public static List<Product> neverOrdered(List<Product> products, List<Order> orders) {
        Set<Long> orderedIds = orders.stream()
                .flatMap(o -> o.getItems().stream())
                .map(item -> item.getProduct().getIdentity())
                .collect(Collectors.toSet());

        return products.stream()
                .filter(p -> !orderedIds.contains(p.getIdentity()))
                .collect(Collectors.toList());
    }

    // Count of products per category — Map<Category, Long>
    public static Map<Category, Long> categoryDistribution(List<Product> products) {
        return products.stream()
                .collect(Collectors.groupingBy(Product::getCategory, Collectors.counting()));
    }

    // Group products into price buckets: "0-50", "50-100", "100-200", "200+"
    // Map<String, List<Product>>
    public static Map<String, List<Product>> priceHistogram(List<Product> products) {
        return products.stream()
                .collect(Collectors.groupingBy(p -> {
                    BigDecimal price = p.getPrice();
                    if (price.compareTo(new BigDecimal("50")) < 0)  return "$0-50";
                    if (price.compareTo(new BigDecimal("100")) < 0) return "$50-100";
                    if (price.compareTo(new BigDecimal("200")) < 0) return "$100-200";
                    return "$200+";
                }));
    }
}
