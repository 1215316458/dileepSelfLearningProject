package com.ecommerce.domain.exception;

public class DuplicateEmailException extends EcommerceException {

    public DuplicateEmailException(String email) {
        super("USER_001", "Email already registered: " + email);
    }
}
