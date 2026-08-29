package com.example.flight.dto;

import com.example.flight.entity.FlightStatus;

import jakarta.validation.constraints.NotNull;

public class FlightStatusRequestDTO {

    @NotNull(message = "Flight status is required")
    private FlightStatus status;

    public FlightStatus getStatus() { return status; }
    public void setStatus(FlightStatus status) { this.status = status; }
}