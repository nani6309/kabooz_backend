package com.kabooz.backend.exception;

/**
 * Thrown when a pricing rule is violated (invalid price for bottle type).
 */
public class InvalidPricingException extends RuntimeException {

    public InvalidPricingException(String message) {
        super(message);
    }
}
