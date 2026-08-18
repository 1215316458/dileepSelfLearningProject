package com.ecommerce.domain.exception;

public class PaymentFailedException extends EcommerceException {

    public PaymentFailedException(String reason) {
        super("PAYMENT_001", "Payment failed: " + reason);
    }
}
