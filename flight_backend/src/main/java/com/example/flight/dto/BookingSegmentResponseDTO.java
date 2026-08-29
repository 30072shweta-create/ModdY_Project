package com.example.flight.dto;

import java.time.LocalDateTime;

public class BookingSegmentResponseDTO {

    private Long segmentId;
    private Long bookingId;
    private Long flightId;
    private String airlineCode;
    private String fromAirport;
    private String toAirport;
    private LocalDateTime departureTs;
    private LocalDateTime arrivalTs;
    private Integer segmentOrder;

    public Long getSegmentId() { return segmentId; }
    public void setSegmentId(Long segmentId) { this.segmentId = segmentId; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Long getFlightId() { return flightId; }
    public void setFlightId(Long flightId) { this.flightId = flightId; }

    public String getAirlineCode() { return airlineCode; }
    public void setAirlineCode(String airlineCode) { this.airlineCode = airlineCode; }

    public String getFromAirport() { return fromAirport; }
    public void setFromAirport(String fromAirport) { this.fromAirport = fromAirport; }

    public String getToAirport() { return toAirport; }
    public void setToAirport(String toAirport) { this.toAirport = toAirport; }

    public LocalDateTime getDepartureTs() { return departureTs; }
    public void setDepartureTs(LocalDateTime departureTs) { this.departureTs = departureTs; }

    public LocalDateTime getArrivalTs() { return arrivalTs; }
    public void setArrivalTs(LocalDateTime arrivalTs) { this.arrivalTs = arrivalTs; }

    public Integer getSegmentOrder() { return segmentOrder; }
    public void setSegmentOrder(Integer segmentOrder) { this.segmentOrder = segmentOrder; }
}