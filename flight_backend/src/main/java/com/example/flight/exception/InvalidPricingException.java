package com.example.flight.exception;

public class InvalidPricingException extends RuntimeException {
    public InvalidPricingException(String message) {
        super(message);
    }
}
