package com.example.flight.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.flight.dto.BookingSegmentRequestDTO;
import com.example.flight.dto.BookingSegmentResponseDTO;
import com.example.flight.service.BookingSegmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(
        "/api/bookings/{bookingId}/segments"
)
@RequiredArgsConstructor
public class BookingSegmentController {

    private final BookingSegmentService
            bookingSegmentService;


    // ================= ADD SEGMENT =================

    @PostMapping
    public ResponseEntity<BookingSegmentResponseDTO>
            addSegment(
                    @PathVariable Long bookingId,

                    @Valid
                    @RequestBody
                    BookingSegmentRequestDTO request,

                    Authentication authentication) {

        String email =
                authentication.getName();

        BookingSegmentResponseDTO response =
                bookingSegmentService.addSegment(
                        bookingId,
                        request,
                        email
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // ================= GET SEGMENTS =================

    @GetMapping
    public ResponseEntity<
            List<BookingSegmentResponseDTO>>
            getSegments(
                    @PathVariable Long bookingId,
                    Authentication authentication) {

        String email =
                authentication.getName();

        return ResponseEntity.ok(
                bookingSegmentService.getSegments(
                        bookingId,
                        email
                )
        );
    }
}