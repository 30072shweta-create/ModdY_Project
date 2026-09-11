package com.example.flight.dto;

import java.time.LocalDateTime;

public class BookingSegmentResponseDTO {

    private Long segmentId;
    private Long bookingId;
    private Long flightId;
    private String flightNumber;
    private String airlineCode;
    private String airlineName;
    private String fromAirport;
    private String toAirport;
    private LocalDateTime departureTs;
    private LocalDateTime arrivalTs;
    private String cabinClass;
    private java.math.BigDecimal price;
    private Integer segmentOrder;

    public Long getSegmentId() { return segmentId; }
    public void setSegmentId(Long segmentId) { this.segmentId = segmentId; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Long getFlightId() { return flightId; }
    public void setFlightId(Long flightId) { this.flightId = flightId; }

    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }

    public String getAirlineCode() { return airlineCode; }
    public void setAirlineCode(String airlineCode) { this.airlineCode = airlineCode; }

    public String getAirlineName() { return airlineName; }
    public void setAirlineName(String airlineName) { this.airlineName = airlineName; }

    public String getFromAirport() { return fromAirport; }
    public void setFromAirport(String fromAirport) { this.fromAirport = fromAirport; }

    public String getToAirport() { return toAirport; }
    public void setToAirport(String toAirport) { this.toAirport = toAirport; }

    public LocalDateTime getDepartureTs() { return departureTs; }
    public void setDepartureTs(LocalDateTime departureTs) { this.departureTs = departureTs; }

    public LocalDateTime getArrivalTs() { return arrivalTs; }
    public void setArrivalTs(LocalDateTime arrivalTs) { this.arrivalTs = arrivalTs; }

    public String getCabinClass() { return cabinClass; }
    public void setCabinClass(String cabinClass) { this.cabinClass = cabinClass; }

    public java.math.BigDecimal getPrice() { return price; }
    public void setPrice(java.math.BigDecimal price) { this.price = price; }

    public Integer getSegmentOrder() { return segmentOrder; }
    public void setSegmentOrder(Integer segmentOrder) { this.segmentOrder = segmentOrder; }
}
