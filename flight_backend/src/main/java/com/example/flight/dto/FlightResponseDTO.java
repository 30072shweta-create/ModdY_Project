package com.example.flight.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.flight.entity.FlightStatus;

public class FlightResponseDTO {

    private Long flightId;
    private String flightNumber;
    private String airlineCode;
    private String airlineName;
    private String fromAirport;
    private String toAirport;
    private Long aircraftId;
    private String aircraftCode;
    private String aircraftModel;
    private Integer totalSeatCapacity;
    private LocalDateTime departureTs;
    private LocalDateTime arrivalTs;
    private Short stops;
    private BigDecimal basePrice;
    private Short availableSeats;
    private Integer durationMins;
    private FlightStatus status;

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

    public Long getAircraftId() { return aircraftId; }
    public void setAircraftId(Long aircraftId) { this.aircraftId = aircraftId; }

    public String getAircraftCode() { return aircraftCode; }
    public void setAircraftCode(String aircraftCode) { this.aircraftCode = aircraftCode; }

    public String getAircraftModel() { return aircraftModel; }
    public void setAircraftModel(String aircraftModel) { this.aircraftModel = aircraftModel; }

    public Integer getTotalSeatCapacity() { return totalSeatCapacity; }
    public void setTotalSeatCapacity(Integer totalSeatCapacity) { this.totalSeatCapacity = totalSeatCapacity; }

    public LocalDateTime getDepartureTs() { return departureTs; }
    public void setDepartureTs(LocalDateTime departureTs) { this.departureTs = departureTs; }

    public LocalDateTime getArrivalTs() { return arrivalTs; }
    public void setArrivalTs(LocalDateTime arrivalTs) { this.arrivalTs = arrivalTs; }

    public Short getStops() { return stops; }
    public void setStops(Short stops) { this.stops = stops; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public Short getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(Short availableSeats) { this.availableSeats = availableSeats; }

    public Integer getDurationMins() { return durationMins; }
    public void setDurationMins(Integer durationMins) { this.durationMins = durationMins; }

    public FlightStatus getStatus() { return status; }
    public void setStatus(FlightStatus status) { this.status = status; }
}