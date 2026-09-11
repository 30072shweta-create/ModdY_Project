package com.example.flight.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.flight.entity.BookingCancellation;

@Repository
public interface CancellationRepository
        extends JpaRepository<BookingCancellation, Long> {

    Optional<BookingCancellation> findByBookingBookingId(
            Long bookingId
    );

    Optional<BookingCancellation> findByBookingBookingCodeIgnoreCase(
            String bookingCode
    );

    boolean existsByBookingBookingId(
            Long bookingId
    );
}