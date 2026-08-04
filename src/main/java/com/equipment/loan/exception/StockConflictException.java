package com.equipment.loan.exception;

// Thrown when the optimistic-lock version check fails because someone else
// updated the same equipment row concurrently. Mapped to HTTP 409.
public class StockConflictException extends RuntimeException {
    public StockConflictException(String message) {
        super(message);
    }
}
