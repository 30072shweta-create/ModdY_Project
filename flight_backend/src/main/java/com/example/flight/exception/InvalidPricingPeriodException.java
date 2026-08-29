package com.example.flight.exception;

public class InvalidPricingPeriodException extends RuntimeException {
    public InvalidPricingPeriodException(String message) {
        super(message);
    }
}
