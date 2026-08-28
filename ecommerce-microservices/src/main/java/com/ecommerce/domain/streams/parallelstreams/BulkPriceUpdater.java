package com.ecommerce.domain.streams.parallelstreams;

import com.ecommerce.domain.model.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

// Delegates to the streams-level BulkPriceUpdater — kept here for package organisation
public class BulkPriceUpdater {

    // Sequential — processes products one by one on the calling thread
    public static long updateSequential(List<Product> products, Function<BigDecimal, BigDecimal> priceFn) {
        long start = System.currentTimeMillis();
        products.forEach(p -> p.setPrice(priceFn.apply(p.getPrice())));
        return System.currentTimeMillis() - start;
    }

    // Parallel — splits work across ForkJoinPool.commonPool() threads
    // Safe here because each thread writes to a DIFFERENT product — no shared mutable state
    public static long updateParallel(List<Product> products, Function<BigDecimal, BigDecimal> priceFn) {
        long start = System.currentTimeMillis();
        products.parallelStream().forEach(p -> p.setPrice(priceFn.apply(p.getPrice())));
        return System.currentTimeMillis() - start;
    }

    public static final Function<BigDecimal, BigDecimal> tenPercentDiscount =
            price -> price.multiply(new BigDecimal("0.90")).setScale(2, RoundingMode.HALF_UP);

    public static final Function<BigDecimal, BigDecimal> restorePrice =
            price -> price.divide(new BigDecimal("0.90"), 2, RoundingMode.HALF_UP);
}
