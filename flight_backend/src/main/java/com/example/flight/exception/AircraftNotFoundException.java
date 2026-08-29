package com.example.flight.exception;

public class AircraftNotFoundException extends RuntimeException {
    public AircraftNotFoundException(String message) {
        super(message);
    }
}
