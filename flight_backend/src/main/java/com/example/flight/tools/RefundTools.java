package com.example.flight.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.example.flight.dto.BookingCancellationResponseDTO;
import com.example.flight.service.BookingCancellationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RefundTools {

    private final BookingCancellationService bookingCancellationService;

    @Tool(description = """
            Get live cancellation and refund status for a booking using either a booking code (e.g. ABC123) or numeric booking ID.
            Use this tool when the user asks about cancellation status, refund status, refund amount,
            cancellation charges, or whether a booking has been cancelled.
            """)
    public BookingCancellationResponseDTO getRefundStatusByBooking(String bookingReference) {
        System.out.println("REFUND TOOL CALLED FOR REFERENCE: " + bookingReference);
        if (bookingReference == null || bookingReference.isBlank()) {
            throw new IllegalArgumentException("Booking reference is required.");
        }
        return bookingCancellationService.getCancellationByBookingReference(bookingReference.trim().replaceAll("[\"']", ""));
    }

    @Tool(description = """
            Calculate the estimated refund amount and cancellation fee if a booking is cancelled now.
            Use this tool when the user asks how much refund they will get if they cancel their flight or booking,
            or what the cancellation charge will be.
            Pass the booking reference code or booking ID.
            """)
    public String calculateEstimatedRefund(String bookingReference) {
        System.out.println("ESTIMATED REFUND TOOL CALLED FOR: " + bookingReference);
        if (bookingReference == null || bookingReference.isBlank()) {
            return "Please provide a booking reference or booking ID to calculate the estimated refund.";
        }
        return bookingCancellationService.calculateEstimatedRefund(bookingReference.trim().replaceAll("[\"']", ""));
    }

    @Tool(description = """
            Get live cancellation and refund information for a booking by numeric booking ID.
            Use this tool when the user provides a numeric booking ID and asks about cancellation or refund status.
            """)
    public BookingCancellationResponseDTO getRefundStatus(
            Long bookingId) {

        System.out.println("REFUND TOOL CALLED FOR ID: " + bookingId);
        if (bookingId == null) {
            throw new IllegalArgumentException("Booking ID is required.");
        }
        return bookingCancellationService
                .getCancellationByBookingId(bookingId);
    }
}