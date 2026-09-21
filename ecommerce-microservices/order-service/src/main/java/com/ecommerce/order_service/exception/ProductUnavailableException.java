package com.ecommerce.order_service.exception;

public class ProductUnavailableException extends RuntimeException {
    public ProductUnavailableException(Long productId) {
        super("Product unavailable (circuit open or service down): " + productId);
    }
}
