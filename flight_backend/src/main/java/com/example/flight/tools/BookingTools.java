package com.example.flight.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.example.flight.dto.BookingResponseDTO;
import com.example.flight.service.BookingService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BookingTools {

    private final BookingService bookingService;

    @Tool(description = """
            Get live booking details using a numeric booking ID.

            Use this tool when the user asks about booking status,
            payment status, booking amount, passengers, or booking details.
            """)
    public BookingResponseDTO getBookingById(Long bookingId) {

        System.out.println("BOOKING BY ID TOOL CALLED");

        return bookingService.getBookingById(bookingId);
    }

    @Tool(description = """
            Get live booking details using a booking code or booking reference (e.g. ABC123).
            Use this tool when the user provides a booking code
            and asks about the booking status, flight, or booking details.
            """)
    public BookingResponseDTO getBookingByCode(String bookingCode) {
        System.out.println("BOOKING BY CODE TOOL CALLED FOR: " + bookingCode);
        if (bookingCode == null || bookingCode.isBlank()) {
            throw new IllegalArgumentException("Booking code is required.");
        }
        String cleanCode = bookingCode.trim().replaceAll("[\"']", "");
        return bookingService.getBookingByCode(cleanCode);
    }
}