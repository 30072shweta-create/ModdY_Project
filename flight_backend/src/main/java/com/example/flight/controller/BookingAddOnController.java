package com.example.flight.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.flight.dto.BookingAddOnRequestDTO;
import com.example.flight.dto.BookingAddOnResponseDTO;
import com.example.flight.service.BookingAddOnService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(
        "/api/bookings/{bookingId}/addons"
)
@RequiredArgsConstructor
public class BookingAddOnController {

    private final BookingAddOnService
            bookingAddOnService;


    // ================= ADD =================

    @PostMapping
    public ResponseEntity<BookingAddOnResponseDTO>
            addAddOn(
                    @PathVariable Long bookingId,
                    @Valid @RequestBody
                    BookingAddOnRequestDTO request,
                    Authentication authentication) {

        String email =
                authentication.getName();

        BookingAddOnResponseDTO response =
                bookingAddOnService.addAddOn(
                        bookingId,
                        request,
                        email
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // ================= GET =================

    @GetMapping
    public ResponseEntity<
            List<BookingAddOnResponseDTO>>
            getBookingAddOns(
                    @PathVariable Long bookingId,
                    Authentication authentication) {

        String email =
                authentication.getName();

        return ResponseEntity.ok(
                bookingAddOnService
                        .getBookingAddOns(
                                bookingId,
                                email
                        )
        );
    }


    // ================= REMOVE =================

    @DeleteMapping("/{addonId}")
    public ResponseEntity<String>
            removeAddOn(
                    @PathVariable Long bookingId,
                    @PathVariable Long addonId,
                    Authentication authentication) {

        String email =
                authentication.getName();

        bookingAddOnService.removeAddOn(
                bookingId,
                addonId,
                email
        );

        return ResponseEntity.ok(
                "Add-on removed successfully"
        );
    }
}