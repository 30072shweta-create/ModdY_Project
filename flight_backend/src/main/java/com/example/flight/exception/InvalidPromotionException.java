package com.example.flight.exception;

public class InvalidPromotionException extends RuntimeException {
    public InvalidPromotionException(String message) {
        super(message);
    }
}
