package com.kabooz.backend.exception;

/**
 * Thrown when a requested order is not found or has been soft-deleted.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long id) {
        super("Order not found with id: " + id);
    }

    public OrderNotFoundException(String message) {
        super(message);
    }
}
