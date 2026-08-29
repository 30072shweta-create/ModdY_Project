package com.example.flight.exception;

public class PaymentAlreadySuccessfulException extends RuntimeException {
    public PaymentAlreadySuccessfulException(String message) {
        super(message);
    }
}
