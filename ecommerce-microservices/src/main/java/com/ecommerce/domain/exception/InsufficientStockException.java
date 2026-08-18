package com.ecommerce.domain.exception;

public class InsufficientStockException extends EcommerceException {

    public InsufficientStockException(Long productId, int requested, int available) {
        super("PRODUCT_002", "Insufficient stock for product " + productId
                + ": requested=" + requested + ", available=" + available);
    }
}
