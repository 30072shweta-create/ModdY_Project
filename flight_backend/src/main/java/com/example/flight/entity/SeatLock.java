package com.example.flight.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "seat_locks",
        schema = "flight_booking_system"
)
@NoArgsConstructor
@AllArgsConstructor
public class SeatLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_lock_id")
    private Long seatLockId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "flight_id",
            nullable = false
    )
    private Flight flight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "booking_id",
            nullable = false
    )
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "segment_id",
            nullable = false
    )
    private BookingSegment bookingSegment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "passenger_id",
            nullable = false
    )
    private Passenger passenger;

    @Column(
            name = "seat_number",
            length = 10,
            nullable = false
    )
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            length = 20,
            nullable = false
    )
    private SeatLockStatus status;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    public Long getSeatLockId() { return seatLockId; }
    public void setSeatLockId(Long seatLockId) { this.seatLockId = seatLockId; }

    public Flight getFlight() { return flight; }
    public void setFlight(Flight flight) { this.flight = flight; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public BookingSegment getBookingSegment() { return bookingSegment; }
    public void setBookingSegment(BookingSegment bookingSegment) { this.bookingSegment = bookingSegment; }

    public Passenger getPassenger() { return passenger; }
    public void setPassenger(Passenger passenger) { this.passenger = passenger; }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public SeatLockStatus getStatus() { return status; }
    public void setStatus(SeatLockStatus status) { this.status = status; }

    public LocalDateTime getLockedAt() { return lockedAt; }
    public void setLockedAt(LocalDateTime lockedAt) { this.lockedAt = lockedAt; }

    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
}