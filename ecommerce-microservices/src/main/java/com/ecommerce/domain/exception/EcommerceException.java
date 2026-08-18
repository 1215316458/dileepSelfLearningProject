package com.ecommerce.domain.exception;

// Abstract base — all ecommerce exceptions share an errorCode.
// Unchecked (extends RuntimeException) — callers aren't forced to catch,
// which suits domain exceptions that represent programming/business errors,
// not recoverable conditions like IOException.
public abstract class EcommerceException extends RuntimeException {

    private final String errorCode;

    protected EcommerceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return "[" + errorCode + "] " + getMessage();
    }
}
