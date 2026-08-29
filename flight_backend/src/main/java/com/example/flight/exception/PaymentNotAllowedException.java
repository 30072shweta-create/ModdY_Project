package com.example.flight.exception;

public class PaymentNotAllowedException extends RuntimeException {
    public PaymentNotAllowedException(String message) {
        super(message);
    }
}
