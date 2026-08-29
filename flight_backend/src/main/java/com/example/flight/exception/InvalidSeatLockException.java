package com.example.flight.exception;

public class InvalidSeatLockException extends RuntimeException {
    public InvalidSeatLockException(String message) {
        super(message);
    }
}
