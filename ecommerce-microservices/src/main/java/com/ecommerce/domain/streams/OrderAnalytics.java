package com.ecommerce.domain.streams;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ecommerce.domain.enums.Category;
import com.ecommerce.domain.enums.OrderStatus;
import com.ecommerce.domain.model.Order;

public class OrderAnalytics {

    // BigDecimal.valueOf(int).multiply(BigDecimal) — can't use * between int and BigDecimal
    public static Map<Category, BigDecimal> revenuePerCategory(List<Order> orders) {
        return orders.stream()
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getCategory(),
                        Collectors.mapping(
                                item -> BigDecimal.valueOf(item.getQuantity()).multiply(item.getProduct().getPrice()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));
    }

     //Monthly revenue trend (groupingBy YearMonth)
     public static Map<YearMonth, BigDecimal> revenuePerYearMonth(List<Order> orders) {
        return orders.stream()
                .collect(Collectors.groupingBy(
                        o -> YearMonth.from(o.getCreatedAt()),
                        Collectors.reducing(BigDecimal.ZERO, Order::getTotalAmount, BigDecimal::add)
                ));
     }



    // Average order value per user — map to totalAmount, reduce to sum, then divide by per-user order count
    public static Map<Long, BigDecimal> averageOrderValuePerUser(List<Order> orders) {
        return orders.stream()
                .collect(Collectors.groupingBy(Order::getUserId))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .map(Order::getTotalAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .divide(BigDecimal.valueOf(e.getValue().size()), 2, RoundingMode.HALF_UP)
                ));
    }

    //High-value customers (total spend > threshold)
    public static List<Long> highValueCustomers(List<Order> orders, BigDecimal threshold) {
        return orders.stream()
                .collect(Collectors.groupingBy(
                        Order::getUserId,
                        Collectors.reducing(BigDecimal.ZERO, Order::getTotalAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .filter(e -> e.getValue().compareTo(threshold) > 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    // Order status distribution — partitioningBy splits into delivered(true) vs not(false)
    // inner groupingBy counts orders per status name
    public static Map<Boolean, Map<String, Long>> orderStatusDistribution(List<Order> orders) {
        return orders.stream()
                .collect(Collectors.partitioningBy(
                        o -> o.getStatus().equals(OrderStatus.DELIVERED),
                        Collectors.groupingBy(o -> o.getStatus().name(), Collectors.counting())
                ));
    }

    // Nested groupingBy: Map<Category, Map<YearMonth, BigDecimal>>
    // outer key = category, inner key = month, value = revenue
    public static Map<Category, Map<YearMonth, BigDecimal>> revenuePerCategoryPerMonth(List<Order> orders) {
        return orders.stream()
                .flatMap(o -> o.getItems().stream()
                        .map(item -> Map.entry(
                                item.getProduct().getCategory(),
                                Map.entry(YearMonth.from(o.getCreatedAt()),
                                        BigDecimal.valueOf(item.getQuantity()).multiply(item.getProduct().getPrice()))
                        ))
                )
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.groupingBy(
                                e -> e.getValue().getKey(),
                                Collectors.reducing(BigDecimal.ZERO, e -> e.getValue().getValue(), BigDecimal::add)
                        )
                ));
    }

    // findCheapestInCategory — Optional so caller must handle "no products in category" case
    public static Optional<Order> getOrderWithHighestValue(Long userId, List<Order> orders) {
        return orders.stream()
                .filter(o -> o.getUserId().equals(userId))
                .max(Comparator.comparing(Order::getTotalAmount));
    }

}
