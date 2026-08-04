package com.equipment.loan.exception;

// Thrown when someone tries to act on a request that isn't at their stage,
// or when their role isn't allowed to act at all (e.g. an EMPLOYEE trying to approve).
public class InvalidStageException extends RuntimeException {
    public InvalidStageException(String message) {
        super(message);
    }
}
