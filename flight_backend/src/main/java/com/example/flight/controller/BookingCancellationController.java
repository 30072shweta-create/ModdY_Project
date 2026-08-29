package com.example.flight.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.flight.dto.BookingCancellationRequestDTO;
import com.example.flight.dto.BookingCancellationResponseDTO;
import com.example.flight.service.BookingAccessService;
import com.example.flight.service.BookingCancellationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cancellations")
@RequiredArgsConstructor
public class BookingCancellationController {

    private final BookingCancellationService cancellationService;

    private final BookingAccessService bookingAccessService;


    // ==========================================
    // CANCEL BOOKING
    // ==========================================

    @PostMapping
    public ResponseEntity<BookingCancellationResponseDTO>
    cancelBooking(
            @Valid @RequestBody
            BookingCancellationRequestDTO dto,

            Authentication authentication
    ) {

        // Check booking ownership
        bookingAccessService.verifyBookingAccess(
                dto.getBookingId(),
                authentication
        );

        String userEmail =
                authentication.getName();

        BookingCancellationResponseDTO response =
                cancellationService.cancelBooking(
                        dto,
                        userEmail
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // ==========================================
    // GET CANCELLATION DETAILS
    // ==========================================

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<BookingCancellationResponseDTO>
    getCancellationByBookingId(
            @PathVariable Long bookingId,
            Authentication authentication
    ) {

        bookingAccessService.verifyBookingAccess(
                bookingId,
                authentication
        );

        return ResponseEntity.ok(
                cancellationService
                        .getCancellationByBookingId(
                                bookingId
                        )
        );
    }
}