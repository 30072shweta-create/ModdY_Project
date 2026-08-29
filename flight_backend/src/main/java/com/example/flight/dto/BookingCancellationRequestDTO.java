package com.example.flight.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class BookingCancellationRequestDTO {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @Size(
            max = 500,
            message = "Cancellation reason must not exceed 500 characters"
    )
    private String reason;

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCancellationReason() {
        return reason;
    }
}
