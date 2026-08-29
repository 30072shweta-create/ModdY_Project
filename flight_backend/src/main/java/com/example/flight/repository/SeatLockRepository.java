package com.example.flight.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.flight.entity.SeatLock;
import com.example.flight.entity.SeatLockStatus;
public interface SeatLockRepository
        extends JpaRepository<SeatLock, Long> {

    Optional<SeatLock>
    findByFlightFlightIdAndSeatNumberAndStatusIn(
            Long flightId,
            String seatNumber,
            List<SeatLockStatus> statuses
    );

    List<SeatLock>
    findByBookingBookingId(
            Long bookingId
    );

    List<SeatLock>
    findByPassengerPassengerId(
            Long passengerId
    );

    List<SeatLock>
    findByStatus(
            SeatLockStatus status
    );

    List<SeatLock>
    findByFlightFlightIdAndStatusIn(
            Long flightId,
            List<SeatLockStatus> statuses
    );
}