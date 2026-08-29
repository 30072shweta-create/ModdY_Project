package com.example.flight.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class BookingSegmentRequestDTO {

    @NotNull(message = "Flight ID is required")
    private Long flightId;

    @NotNull(message = "Segment order is required")
    @Positive(message = "Segment order must be positive")
    private Integer segmentOrder;

    public Long getFlightId() { return flightId; }
    public void setFlightId(Long flightId) { this.flightId = flightId; }

    public Integer getSegmentOrder() { return segmentOrder; }
    public void setSegmentOrder(Integer segmentOrder) { this.segmentOrder = segmentOrder; }
}