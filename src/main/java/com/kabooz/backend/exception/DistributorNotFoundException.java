package com.kabooz.backend.exception;

/**
 * Thrown when a distributor with the requested id does not exist.
 */
public class DistributorNotFoundException extends RuntimeException {
    public DistributorNotFoundException(Long id) {
        super("Distributor not found with id: " + id);
    }
}

