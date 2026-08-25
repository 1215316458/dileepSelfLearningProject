package com.ecommerce.domain.streams;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SalesReport {

    private final BigDecimal totalRevenue;
    private final int orderCount;
    private final BigDecimal averageOrderValue;
    // productId -> total quantity sold, sorted descending
    private final List<Map.Entry<Long, Integer>> topProducts;

    public SalesReport(BigDecimal totalRevenue, int orderCount, List<Map.Entry<Long, Integer>> topProducts) {
        this.totalRevenue = totalRevenue;
        this.orderCount = orderCount;
        // avoid divide-by-zero when no orders
        this.averageOrderValue = orderCount == 0 ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);
        this.topProducts = Collections.unmodifiableList(topProducts);
    }

    public BigDecimal getTotalRevenue()          { return totalRevenue; }
    public int getOrderCount()                   { return orderCount; }
    public BigDecimal getAverageOrderValue()     { return averageOrderValue; }
    public List<Map.Entry<Long, Integer>> getTopProducts() { return topProducts; }

    @Override
    public String toString() {
        return new StringBuilder("SalesReport{")
                .append("totalRevenue=$").append(totalRevenue)
                .append(", orderCount=").append(orderCount)
                .append(", averageOrderValue=$").append(averageOrderValue)
                .append(", topProducts=").append(topProducts)
                .append('}')
                .toString();
    }
}
