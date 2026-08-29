package com.example.flight.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.flight.dto.BookingRequestDTO;
import com.example.flight.dto.BookingCancellationRequestDTO;
import com.example.flight.dto.BookingResponseDTO;
import com.example.flight.service.BookingAccessService;
import com.example.flight.service.BookingCancellationService;
import com.example.flight.service.BookingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingAccessService bookingAccessService;
    private final BookingCancellationService cancellationService;

    // Get all bookings (ADMIN only)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BookingResponseDTO>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    // Get booking by ID (Owner or ADMIN)
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponseDTO> getBookingById(
            @PathVariable Long bookingId,
            Authentication authentication) {
        bookingAccessService.verifyBookingAccess(bookingId, authentication);
        return ResponseEntity.ok(bookingService.getBookingById(bookingId));
    }

    // Get booking by booking code (Owner or ADMIN)
    @GetMapping("/code/{bookingCode}")
    public ResponseEntity<BookingResponseDTO> getBookingByCode(
            @PathVariable String bookingCode,
            Authentication authentication) {
        bookingAccessService.verifyBookingCodeAccess(bookingCode, authentication);
        return ResponseEntity.ok(bookingService.getBookingByCode(bookingCode));
    }

    // Get bookings by user (Self or ADMIN)
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByUser(
            @PathVariable Long userId,
            Authentication authentication) {
        bookingAccessService.verifyUserAccess(userId, authentication);
        return ResponseEntity.ok(bookingService.getBookingsByUser(userId));
    }

    // Get bookings by flight (ADMIN only)
    @GetMapping("/flight/{flightId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByFlight(
            @PathVariable Long flightId) {
        return ResponseEntity.ok(bookingService.getBookingsByFlight(flightId));
    }

    // Create booking
    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(
            @Valid @RequestBody BookingRequestDTO dto,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(bookingService.createBooking(dto, email));
    }

    // Confirm booking (Owner or ADMIN)
    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<BookingResponseDTO> confirmBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {
        bookingAccessService.verifyBookingAccess(bookingId, authentication);
        String email = authentication.getName();
        return ResponseEntity.ok(bookingService.confirmBooking(bookingId, email));
    }

    // Cancel booking (Owner or ADMIN)
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Object> cancelBooking(
            @PathVariable Long bookingId,
            @RequestBody(required = false) BookingCancellationRequestDTO requestDTO,
            Authentication authentication) {
        bookingAccessService.verifyBookingAccess(bookingId, authentication);

        if (requestDTO == null) {
            requestDTO = new BookingCancellationRequestDTO();
        }

        requestDTO.setBookingId(bookingId);

        if (requestDTO.getReason() == null || requestDTO.getReason().isBlank()) {
            requestDTO.setReason("User requested cancellation");
        }

        String email = authentication.getName();
        return ResponseEntity.ok(cancellationService.cancelBooking(requestDTO, email));
    }
}
