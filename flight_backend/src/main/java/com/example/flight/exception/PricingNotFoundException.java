package com.example.flight.exception;

public class PricingNotFoundException extends RuntimeException {
    public PricingNotFoundException(String message) {
        super(message);
    }
}
