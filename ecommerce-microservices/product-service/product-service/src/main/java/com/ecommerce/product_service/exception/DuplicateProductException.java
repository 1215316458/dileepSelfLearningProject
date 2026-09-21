package com.ecommerce.product_service.exception;

public class DuplicateProductException extends RuntimeException {

    public DuplicateProductException(String name) {
        super("Product already exists with name: " + name);
    }
}
