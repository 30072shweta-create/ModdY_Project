package com.example.flight.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.flight.dto.AirportRequestDTO;
import com.example.flight.dto.AirportResponseDTO;
import com.example.flight.service.AirportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/airports")
@RequiredArgsConstructor
public class AirportController {

    private final AirportService airportService;


    // ================= ADMIN =================

    // ADD AIRPORT

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public AirportResponseDTO addAirport(
            @Valid @RequestBody AirportRequestDTO request) {

        return airportService.addAirport(request);
    }


    // UPDATE AIRPORT

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{airportCode}")
    public AirportResponseDTO updateAirport(
            @PathVariable String airportCode,
            @Valid @RequestBody AirportRequestDTO request) {

        return airportService.updateAirport(
                airportCode,
                request
        );
    }


    // DELETE AIRPORT

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{airportCode}")
    public ResponseEntity<String> deleteAirport(
            @PathVariable String airportCode) {

        airportService.deleteAirport(airportCode);

        return ResponseEntity.ok(
                "Airport deleted successfully"
        );
    }


    // ================= USER + ADMIN =================

    // GET ALL AIRPORTS

    @GetMapping
    public List<AirportResponseDTO> getAllAirports() {

        return airportService.getAllAirports();
    }


    // GET AIRPORT BY CODE

    @GetMapping("/{airportCode}")
    public AirportResponseDTO getAirportByCode(
            @PathVariable String airportCode) {

        return airportService.getAirportByCode(
                airportCode
        );
    }
}