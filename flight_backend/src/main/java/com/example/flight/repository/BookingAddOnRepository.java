package com.example.flight.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.flight.entity.BookingAddOn;

public interface BookingAddOnRepository
        extends JpaRepository<BookingAddOn, Long> {

    List<BookingAddOn>
    findByBookingBookingId(Long bookingId);
}