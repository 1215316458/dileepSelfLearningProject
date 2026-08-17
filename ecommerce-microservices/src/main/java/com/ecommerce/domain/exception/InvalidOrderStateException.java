package com.ecommerce.domain.exception;

// Unchecked — caller doesn't have to declare it, but it signals a programming error (invalid state machine usage)
public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
