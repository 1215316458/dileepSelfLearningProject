package com.ecommerce.domain.exception;

public class InvalidOrderStateException extends EcommerceException {

    public InvalidOrderStateException(String message) {
        super("ORDER_001", message);
    }
}
