package com.example.flight.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.flight.entity.CabinClass;
import com.example.flight.entity.FlightPricing;

@Repository
public interface FlightPricingRepository extends JpaRepository<FlightPricing, Long> {

    Optional<FlightPricing> findByFlightFlightIdAndSeatClass(
            Long flightId,
            CabinClass seatClass
    );

    @Query("SELECT fp FROM FlightPricing fp WHERE fp.flight.flightId = :flightId AND fp.seatClass = :seatClass AND fp.effectiveFrom <= :timestamp AND (fp.effectiveTo IS NULL OR fp.effectiveTo >= :timestamp) ORDER BY fp.effectiveFrom DESC, fp.pricingId DESC")
    List<FlightPricing> findActivePricingList(
            @Param("flightId") Long flightId,
            @Param("seatClass") CabinClass seatClass,
            @Param("timestamp") LocalDateTime timestamp
    );

    default Optional<FlightPricing> findActivePricing(Long flightId, CabinClass seatClass, LocalDateTime timestamp) {
        List<FlightPricing> list = findActivePricingList(flightId, seatClass, timestamp);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    List<FlightPricing> findByFlightFlightIdAndSeatClassOrderByEffectiveFromDesc(
            Long flightId,
            CabinClass seatClass
    );

    List<FlightPricing> findByFlightFlightIdOrderByEffectiveFromDesc(
            Long flightId
    );

    @Query("SELECT fp FROM FlightPricing fp WHERE fp.flight.flightId = :flightId AND fp.seatClass = :seatClass AND (fp.effectiveTo IS NULL OR fp.effectiveTo >= :from) AND (:to IS NULL OR fp.effectiveFrom <= :to)")
    List<FlightPricing> findOverlappingPricing(
            @Param("flightId") Long flightId,
            @Param("seatClass") CabinClass seatClass,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}