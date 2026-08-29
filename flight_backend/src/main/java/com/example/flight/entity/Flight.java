package com.example.flight.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "flights",
    indexes = {
        @Index(name = "idx_flight_number", columnList = "flight_number"),
        @Index(name = "idx_flight_from_airport", columnList = "from_airport"),
        @Index(name = "idx_flight_to_airport", columnList = "to_airport"),
        @Index(name = "idx_flight_departure_ts", columnList = "departure_ts"),
        @Index(name = "idx_flight_status", columnList = "status"),
        @Index(name = "idx_flight_aircraft_id", columnList = "aircraft_id")
    }
)
@NoArgsConstructor
@AllArgsConstructor
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flight_id")
    private Long flightId;

    @Column(name = "flight_number", nullable = false, unique = true)
    private String flightNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "airline_code",
        referencedColumnName = "airline_code"
    )
    private Airline airline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "from_airport",
        referencedColumnName = "airport_code"
    )
    private Airport fromAirport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "to_airport",
        referencedColumnName = "airport_code"
    )
    private Airport toAirport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "aircraft_id",
        referencedColumnName = "aircraft_id"
    )
    private Aircraft aircraft;

    @Column(name = "departure_ts", nullable = false)
    private LocalDateTime departureTs;

    @Column(name = "arrival_ts", nullable = false)
    private LocalDateTime arrivalTs;

    @Column(name = "stops")
    private Short stops;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    @Column(name = "available_seats", nullable = false)
    private Short availableSeats;

    @Column(name = "duration_mins")
    private Integer durationMins;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FlightStatus status;

    public Long getFlightId() { return flightId; }
    public void setFlightId(Long flightId) { this.flightId = flightId; }

    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }

    public Airline getAirline() { return airline; }
    public void setAirline(Airline airline) { this.airline = airline; }

    public Airport getFromAirport() { return fromAirport; }
    public void setFromAirport(Airport fromAirport) { this.fromAirport = fromAirport; }

    public Airport getToAirport() { return toAirport; }
    public void setToAirport(Airport toAirport) { this.toAirport = toAirport; }

    public Aircraft getAircraft() { return aircraft; }
    public void setAircraft(Aircraft aircraft) { this.aircraft = aircraft; }

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