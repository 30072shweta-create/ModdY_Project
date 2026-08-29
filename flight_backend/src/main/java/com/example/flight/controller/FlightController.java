package com.example.flight.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.flight.dto.FlightRequestDTO;
import com.example.flight.dto.FlightResponseDTO;
import com.example.flight.dto.FlightSearchRequestDTO;
import com.example.flight.dto.FlightStatusRequestDTO;
import com.example.flight.service.FlightService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    @Autowired
    private FlightService flightService;

    // Add a new flight to admin panel
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public FlightResponseDTO addFlight(
            @Valid @RequestBody FlightRequestDTO flightRequestDTO) {

        return flightService.addFlight(flightRequestDTO);
    }

    // Get all flights to user panel
    @GetMapping
    public List<FlightResponseDTO> getAllFlights() {
        return flightService.getAllFlights();
    }

    // Search flights to user panel
    @GetMapping("/search")
    public Page<FlightResponseDTO> searchFlights(
            @ModelAttribute FlightSearchRequestDTO request) {

        return flightService.searchFlights(request);
    }

    //to admin panel
    @GetMapping("/{flightId}")
    public FlightResponseDTO getFlightById(
            @PathVariable Long flightId) {

        return flightService.getFlightById(flightId);
    }

    //to admin panel
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{flightId}")
    public FlightResponseDTO updateFlight(
            @PathVariable Long flightId,
            @Valid @RequestBody FlightRequestDTO flightRequestDTO) {

    return flightService.updateFlight(
            flightId,
            flightRequestDTO
    );
}
//to admin panel
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{flightId}")
public ResponseEntity<String> deleteFlight(
        @PathVariable Long flightId) {

    flightService.deleteFlight(flightId);

    return ResponseEntity.ok(
            "Flight deleted successfully"
    );
}
//to admin panel
@PreAuthorize("hasRole('ADMIN')")
@PatchMapping("/{flightId}/status")
public FlightResponseDTO updateFlightStatus(
        @PathVariable Long flightId,
        @Valid @RequestBody FlightStatusRequestDTO request) {

    return flightService.updateFlightStatus(
            flightId,
            request
    );
}
}
