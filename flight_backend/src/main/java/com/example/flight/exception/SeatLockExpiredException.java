package com.example.flight.exception;

public class SeatLockExpiredException extends RuntimeException {
    public SeatLockExpiredException(String message) {
        super(message);
    }
}
