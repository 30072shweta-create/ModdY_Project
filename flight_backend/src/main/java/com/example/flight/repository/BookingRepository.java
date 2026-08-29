package com.example.flight.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.flight.entity.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingCode(String bookingCode);

    boolean existsByBookingCode(String bookingCode);

    List<Booking> findByUserUserId(Long userId);

    List<Booking> findByFlightFlightId(Long flightId);

    @Query("SELECT DISTINCT b FROM Booking b LEFT JOIN FETCH b.user LEFT JOIN FETCH b.flight f LEFT JOIN FETCH f.airline LEFT JOIN FETCH f.fromAirport LEFT JOIN FETCH f.toAirport LEFT JOIN FETCH f.aircraft LEFT JOIN FETCH b.segments s WHERE b.bookingId = :bookingId")
    Optional<Booking> findByIdWithDetails(@Param("bookingId") Long bookingId);

    @Query("SELECT DISTINCT b FROM Booking b LEFT JOIN FETCH b.user LEFT JOIN FETCH b.flight f LEFT JOIN FETCH f.airline LEFT JOIN FETCH f.fromAirport LEFT JOIN FETCH f.toAirport LEFT JOIN FETCH f.aircraft LEFT JOIN FETCH b.segments s WHERE b.user.userId = :userId")
    List<Booking> findByUserUserIdWithDetails(@Param("userId") Long userId);
}
