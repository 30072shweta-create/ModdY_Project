package com.example.flight.dto;

import jakarta.validation.constraints.NotNull;

public class SeatLockRequestDTO {

    @NotNull(message = "Segment ID is required")
    private Long segmentId;

    @NotNull(message = "Passenger ID is required")
    private Long passengerId;

    public Long getSegmentId() { return segmentId; }
    public void setSegmentId(Long segmentId) { this.segmentId = segmentId; }

    public Long getPassengerId() { return passengerId; }
    public void setPassengerId(Long passengerId) { this.passengerId = passengerId; }
}