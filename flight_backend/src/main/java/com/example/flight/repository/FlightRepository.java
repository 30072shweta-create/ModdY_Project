package com.example.flight.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.flight.entity.Flight;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    @Query("SELECT f FROM Flight f LEFT JOIN FETCH f.airline LEFT JOIN FETCH f.fromAirport LEFT JOIN FETCH f.toAirport LEFT JOIN FETCH f.aircraft WHERE f.flightId = :flightId")
    Optional<Flight> findByIdWithDetails(@Param("flightId") Long flightId);

    @Query("""
        SELECT f
        FROM Flight f
        LEFT JOIN FETCH f.airline
        LEFT JOIN FETCH f.fromAirport
        LEFT JOIN FETCH f.toAirport
        LEFT JOIN FETCH f.aircraft
        WHERE
            (COALESCE(:source, '') = '' OR UPPER(f.fromAirport.airportCode) = UPPER(:source))
        AND
            (COALESCE(:destination, '') = '' OR UPPER(f.toAirport.airportCode) = UPPER(:destination))
        AND
            (f.departureTs >= :startDateTime AND f.departureTs < :endDateTime)
        AND
            (COALESCE(:airline, '') = '' OR UPPER(f.airline.airlineCode) = UPPER(:airline))
        AND
            (COALESCE(:flightNumber, '') = '' OR UPPER(f.flightNumber) = UPPER(:flightNumber))
        AND
            (:stops IS NULL OR f.stops = :stops)
        AND
            (:minPrice IS NULL OR f.basePrice >= :minPrice)
        AND
            (:maxPrice IS NULL OR f.basePrice <= :maxPrice)
        AND
            (:maxDuration IS NULL OR f.durationMins <= :maxDuration)
        """)
    Page<Flight> searchFlights(
            @Param("source") String source,
            @Param("destination") String destination,
            @Param("date") LocalDate date,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("airline") String airline,
            @Param("flightNumber") String flightNumber,
            @Param("stops") Short stops,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("maxDuration") Integer maxDuration,
            Pageable pageable
    );
}