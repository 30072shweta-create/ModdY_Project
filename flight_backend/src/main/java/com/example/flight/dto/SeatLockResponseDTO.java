package com.example.flight.dto;

import java.time.LocalDateTime;
import com.example.flight.entity.SeatLockStatus;

public class SeatLockResponseDTO {

    private Long seatLockId;
    private Long bookingId;
    private Long segmentId;
    private Long flightId;
    private Long passengerId;
    private String seatNumber;
    private SeatLockStatus status;
    private LocalDateTime lockedAt;
    private LocalDateTime lockedUntil;

    public Long getSeatLockId() { return seatLockId; }
    public void setSeatLockId(Long seatLockId) { this.seatLockId = seatLockId; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Long getSegmentId() { return segmentId; }
    public void setSegmentId(Long segmentId) { this.segmentId = segmentId; }

    public Long getFlightId() { return flightId; }
    public void setFlightId(Long flightId) { this.flightId = flightId; }

    public Long getPassengerId() { return passengerId; }
    public void setPassengerId(Long passengerId) { this.passengerId = passengerId; }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public SeatLockStatus getStatus() { return status; }
    public void setStatus(SeatLockStatus status) { this.status = status; }

    public LocalDateTime getLockedAt() { return lockedAt; }
    public void setLockedAt(LocalDateTime lockedAt) { this.lockedAt = lockedAt; }

    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
}