package com.ecommerce.domain.exception;

public class ProductNotFoundException extends EcommerceException {

    public ProductNotFoundException(Long productId) {
        super("PRODUCT_001", "Product not found with id: " + productId);
    }
}
