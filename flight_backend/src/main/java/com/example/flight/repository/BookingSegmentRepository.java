package com.example.flight.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.flight.entity.BookingSegment;

public interface BookingSegmentRepository
        extends JpaRepository<BookingSegment, Long> {

    List<BookingSegment>
    findByBookingBookingIdOrderBySegmentOrderAsc(
            Long bookingId
    );
}