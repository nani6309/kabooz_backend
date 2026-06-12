package com.kabooz.backend.exception;

/**
 * Thrown when authentication fails or token is invalid/expired.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
