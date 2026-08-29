package com.example.flight.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.flight.entity.Passenger;

public interface PassengerRepository
        extends JpaRepository<Passenger, Long> {

    List<Passenger> findByBookingBookingId(Long bookingId);
}