package com.ecommerce.domain.streams;

import com.ecommerce.domain.model.Order;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class SalesReportCollector implements Collector<Order, SalesReportCollector.Accumulator, SalesReport> {

    // Mutable container that accumulates state during collection
    static class Accumulator {
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int orderCount = 0;
        // productId -> total quantity sold
        final Map<Long, Integer> productQuantities = new HashMap<>();
    }

    // supplier — creates a fresh empty Accumulator for each stream (or each parallel segment)
    @Override
    public Supplier<Accumulator> supplier() {
        return Accumulator::new;
    }

    // accumulator — folds one Order into the Accumulator
    @Override
    public BiConsumer<Accumulator, Order> accumulator() {
        return (acc, order) -> {
            acc.totalRevenue = acc.totalRevenue.add(order.getTotalAmount());
            acc.orderCount++;
            order.getItems().forEach(item ->
                    acc.productQuantities.merge(
                            item.getProduct().getIdentity(),
                            item.getQuantity(),
                            Integer::sum
                    )
            );
        };
    }

    // combiner — merges two Accumulators from parallel segments into one
    @Override
    public BinaryOperator<Accumulator> combiner() {
        return (a, b) -> {
            a.totalRevenue = a.totalRevenue.add(b.totalRevenue);
            a.orderCount += b.orderCount;
            b.productQuantities.forEach((id, qty) -> a.productQuantities.merge(id, qty, Integer::sum));
            return a;
        };
    }

    // finisher — transforms the Accumulator into the final SalesReport
    // sort productQuantities by value descending to get top products
    @Override
    public Function<Accumulator, SalesReport> finisher() {
        return acc -> {
            List<Map.Entry<Long, Integer>> topProducts = acc.productQuantities.entrySet().stream()
                    .sorted(Map.Entry.<Long, Integer>comparingByValue(Comparator.reverseOrder()))
                    .collect(Collectors.toList());
            return new SalesReport(acc.totalRevenue, acc.orderCount, topProducts);
        };
    }

    // UNORDERED — result doesn't depend on encounter order, allows parallel optimisation
    @Override
    public Set<Characteristics> characteristics() {
        return Collections.singleton(Characteristics.UNORDERED);
    }
}
