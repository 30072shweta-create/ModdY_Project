package com.example.flight.controller;

import com.example.flight.dto.SeatLockRequestDTO;
import com.example.flight.dto.SeatLockResponseDTO;
import com.example.flight.service.SeatLockService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seat-locks")
@RequiredArgsConstructor
public class SeatLockController {

    private final SeatLockService seatLockService;

    // Automatically allocate and lock seat
    @PostMapping
    public ResponseEntity<SeatLockResponseDTO> allocateAndLockSeat(
            @Valid @RequestBody SeatLockRequestDTO request,
            Authentication authentication) {

        String email = authentication.getName();
        SeatLockResponseDTO response = seatLockService.allocateAndLockSeat(request, email);
        return ResponseEntity.ok(response);
    }

    // Lock a specific seat requested by user
    @PostMapping("/lock-specific")
    public ResponseEntity<SeatLockResponseDTO> lockSpecificSeat(
            @RequestParam Long flightId,
            @RequestParam Long segmentId,
            @RequestParam String seatNumber,
            @RequestParam Long passengerId,
            Authentication authentication) {

        String email = authentication.getName();
        SeatLockResponseDTO response = seatLockService.lockSpecificSeat(
                flightId, segmentId, seatNumber, passengerId, email);
        return ResponseEntity.ok(response);
    }

    // Release seat lock by owner
    @DeleteMapping("/release")
    public ResponseEntity<Map<String, Object>> releaseSeatLock(
            @RequestParam Long flightId,
            @RequestParam String seatNumber,
            Authentication authentication) {

        String email = authentication.getName();
        boolean released = seatLockService.releaseSeatLock(flightId, seatNumber, email);
        return ResponseEntity.ok(Map.of("released", released, "message", "Seat lock released successfully"));
    }

    // Check Redis seat lock status
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkSeatLock(
            @RequestParam Long flightId,
            @RequestParam String seatNumber) {

        boolean isLocked = seatLockService.isSeatLockedInRedis(flightId, seatNumber);
        String owner = seatLockService.getSeatLockOwner(flightId, seatNumber);
        return ResponseEntity.ok(Map.of(
                "flightId", flightId,
                "seatNumber", seatNumber,
                "isLocked", isLocked,
                "owner", owner != null ? owner : "none"
        ));
    }

    // Get all seat locks for a booking
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<SeatLockResponseDTO>> getBookingSeatLocks(
            @PathVariable Long bookingId) {

        List<SeatLockResponseDTO> response = seatLockService.getBookingSeatLocks(bookingId);
        return ResponseEntity.ok(response);
    }

    // Get all occupied (permanently booked or Redis locked) seats for a flight
    @GetMapping("/flight/{flightId}/occupied")
    public ResponseEntity<List<Map<String, Object>>> getOccupiedSeatsForFlight(
            @PathVariable Long flightId,
            Authentication authentication) {

        String email = authentication != null ? authentication.getName() : null;
        List<Map<String, Object>> response = seatLockService.getOccupiedSeatsForFlight(flightId, email);
        return ResponseEntity.ok(response);
    }
}