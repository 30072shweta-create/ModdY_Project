package com.example.flight.exception;

public class InvalidAircraftException extends RuntimeException {
    public InvalidAircraftException(String message) {
        super(message);
    }
}
