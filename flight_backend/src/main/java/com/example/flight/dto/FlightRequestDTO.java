package com.example.flight.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.flight.entity.FlightStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FlightRequestDTO {

    @NotBlank(message = "Flight number is required")
    @Size(max = 20, message = "Flight number cannot exceed 20 characters")
    private String flightNumber;

    @NotBlank(message = "Airline code is required")
    @Size(max = 10, message = "Airline code cannot exceed 10 characters")
    private String airlineCode;

    @NotBlank(message = "Origin airport is required")
    @Size(max = 10, message = "Airport code cannot exceed 10 characters")
    private String fromAirport;

    @NotBlank(message = "Destination airport is required")
    @Size(max = 10, message = "Airport code cannot exceed 10 characters")
    private String toAirport;

    private Long aircraftId;

    @NotNull(message = "Departure time is required")
    private LocalDateTime departureTs;

    @NotNull(message = "Arrival time is required")
    private LocalDateTime arrivalTs;

    @Min(value = 0, message = "Stops cannot be negative")
    private Short stops;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", message = "Base price cannot be negative")
    private BigDecimal basePrice;

    @NotNull(message = "Available seats count is required")
    @Min(value = 0, message = "Available seats cannot be negative")
    private Short availableSeats;

    @Min(value = 0, message = "Duration cannot be negative")
    private Integer durationMins;

    @NotNull(message = "Flight status is required")
    private FlightStatus status;

    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }

    public String getAirlineCode() { return airlineCode; }
    public void setAirlineCode(String airlineCode) { this.airlineCode = airlineCode; }

    public String getFromAirport() { return fromAirport; }
    public void setFromAirport(String fromAirport) { this.fromAirport = fromAirport; }

    public String getToAirport() { return toAirport; }
    public void setToAirport(String toAirport) { this.toAirport = toAirport; }

    public Long getAircraftId() { return aircraftId; }
    public void setAircraftId(Long aircraftId) { this.aircraftId = aircraftId; }

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