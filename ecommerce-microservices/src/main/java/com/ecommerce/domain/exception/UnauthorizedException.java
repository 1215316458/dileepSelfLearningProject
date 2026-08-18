package com.ecommerce.domain.exception;

public class UnauthorizedException extends EcommerceException {

    public UnauthorizedException(String action) {
        super("AUTH_001", "Unauthorized to perform action: " + action);
    }
}
