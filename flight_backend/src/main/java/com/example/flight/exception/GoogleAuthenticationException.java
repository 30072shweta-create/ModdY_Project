package com.example.flight.exception;

public class GoogleAuthenticationException extends RuntimeException {
    public GoogleAuthenticationException(String message) {
        super(message);
    }

    public GoogleAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
