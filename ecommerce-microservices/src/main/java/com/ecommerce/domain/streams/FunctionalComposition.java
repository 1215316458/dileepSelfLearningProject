package com.ecommerce.domain.streams;

import com.ecommerce.domain.enums.Category;
import com.ecommerce.domain.enums.OrderStatus;
import com.ecommerce.domain.model.CartItem;
import com.ecommerce.domain.model.Order;
import com.ecommerce.domain.model.Product;
import com.ecommerce.domain.streams.filter.ProductFilter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class FunctionalComposition {

    // Function.andThen — chains two Functions: output of first becomes input of second
    // applyDiscount → roundPrice: apply 10% discount, then round to 2 decimal places
    public static final Function<BigDecimal, BigDecimal> applyDiscountThenRound =
            ((Function<BigDecimal, BigDecimal>) price -> price.multiply(new BigDecimal("0.90")))
            .andThen(price -> price.setScale(2, RoundingMode.HALF_UP));

    // Function.compose — opposite of andThen: roundPrice first, then applyDiscount
    // compose(f) means: apply f first, then apply this
    public static final Function<BigDecimal, BigDecimal> roundThenApplyDiscount =
            ((Function<BigDecimal, BigDecimal>) price -> price.multiply(new BigDecimal("0.90")))
            .compose(price -> price.setScale(2, RoundingMode.HALF_UP));

    // Predicate.and — both must be true (short-circuits on first false)
    public static final Predicate<Product> affordableElectronicsInStock =
            ProductFilter.byCategory(Category.ELECTRONICS)
            .and(ProductFilter.byInStock())
            .and(ProductFilter.byPriceRange(new BigDecimal("0"), new BigDecimal("500")));

    // Predicate.or — either must be true
    public static final Predicate<Product> booksOrSports =
            ProductFilter.byCategory(Category.BOOKS)
            .or(ProductFilter.byCategory(Category.SPORTS));

    // Predicate.negate — inverts the result
    public static final Predicate<Product> notOutOfStock =
            ProductFilter.byInStock().negate().negate(); // double negate = same as byInStock, just for demo

    // Supplier<Order> — factory that creates a test order on demand
    // Supplier defers creation: the Order is only built when get() is called
    public static Supplier<Order> testOrderSupplier(Product product) {
        return () -> new Order.Builder()
                .id(System.currentTimeMillis())
                .userId(1L)
                .addItem(new CartItem(product, 1))
                .status(OrderStatus.PENDING)
                .shippingAddress("Test Address")
                .build();
    }
}
