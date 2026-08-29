package com.example.flight.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.flight.dto.PassengerRequestDTO;
import com.example.flight.dto.PassengerResponseDTO;
import com.example.flight.service.BookingAccessService;
import com.example.flight.service.PassengerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookings/{bookingId}/passengers")
@RequiredArgsConstructor
public class PassengerController {

    private final PassengerService passengerService;
    private final BookingAccessService bookingAccessService;

    // ================= ADD PASSENGER =================
    @PostMapping
    public ResponseEntity<PassengerResponseDTO> addPassenger(
            @PathVariable Long bookingId,
            @Valid @RequestBody PassengerRequestDTO request,
            Authentication authentication) {

        bookingAccessService.verifyBookingAccess(bookingId, authentication);
        String email = authentication.getName();

        PassengerResponseDTO response = passengerService.addPassenger(
                bookingId,
                request,
                email
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ================= GET PASSENGERS =================
    @GetMapping
    public ResponseEntity<List<PassengerResponseDTO>> getPassengers(
            @PathVariable Long bookingId,
            Authentication authentication) {

        bookingAccessService.verifyBookingAccess(bookingId, authentication);
        String email = authentication.getName();

        return ResponseEntity.ok(
                passengerService.getPassengers(
                        bookingId,
                        email
                )
        );
    }
}