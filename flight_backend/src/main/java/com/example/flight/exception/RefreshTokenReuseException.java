package com.example.flight.exception;

public class RefreshTokenReuseException extends RuntimeException {
    public RefreshTokenReuseException(String message) {
        super(message);
    }

    public RefreshTokenReuseException(String message, Throwable cause) {
        super(message, cause);
    }
}
