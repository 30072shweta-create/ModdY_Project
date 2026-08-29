package com.example.flight.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.flight.dto.AirlineRequestDTO;
import com.example.flight.dto.AirlineResponseDTO;
import com.example.flight.service.AirlineService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/airlines")
@RequiredArgsConstructor
public class AirlineController {

    private final AirlineService airlineService;


    // ================= ADMIN =================

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<AirlineResponseDTO> addAirline(
            @Valid @RequestBody AirlineRequestDTO request) {

        AirlineResponseDTO response =
                airlineService.addAirline(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{airlineCode}")
    public ResponseEntity<AirlineResponseDTO> updateAirline(
            @PathVariable String airlineCode,
            @Valid @RequestBody AirlineRequestDTO request) {

        AirlineResponseDTO response =
                airlineService.updateAirline(
                        airlineCode,
                        request
                );

        return ResponseEntity.ok(response);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{airlineCode}")
    public ResponseEntity<String> deleteAirline(
            @PathVariable String airlineCode) {

        airlineService.deleteAirline(airlineCode);

        return ResponseEntity.ok(
                "Airline deleted successfully"
        );
    }


    // ================= USER + ADMIN =================

    @GetMapping
    public ResponseEntity<List<AirlineResponseDTO>>
            getAllAirlines() {

        return ResponseEntity.ok(
                airlineService.getAllAirlines()
        );
    }


    @GetMapping("/{airlineCode}")
    public ResponseEntity<AirlineResponseDTO>
            getAirlineByCode(
                    @PathVariable String airlineCode) {

        return ResponseEntity.ok(
                airlineService.getAirlineByCode(
                        airlineCode
                )
        );
    }
}