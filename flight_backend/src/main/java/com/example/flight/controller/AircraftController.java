package com.example.flight.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.flight.dto.AircraftRequestDTO;
import com.example.flight.dto.AircraftResponseDTO;
import com.example.flight.service.AircraftService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/aircraft")
@RequiredArgsConstructor
public class AircraftController {

    private final AircraftService aircraftService;

    @GetMapping
    public ResponseEntity<List<AircraftResponseDTO>> getAllAircraft() {
        return ResponseEntity.ok(aircraftService.getAllAircraft());
    }

    @GetMapping("/{aircraftId}")
    public ResponseEntity<AircraftResponseDTO> getAircraftById(@PathVariable Long aircraftId) {
        return ResponseEntity.ok(aircraftService.getAircraftById(aircraftId));
    }

    @GetMapping("/code/{aircraftCode}")
    public ResponseEntity<AircraftResponseDTO> getAircraftByCode(@PathVariable String aircraftCode) {
        return ResponseEntity.ok(aircraftService.getAircraftByCode(aircraftCode));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AircraftResponseDTO> createAircraft(@Valid @RequestBody AircraftRequestDTO dto) {
        AircraftResponseDTO created = aircraftService.createAircraft(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{aircraftId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AircraftResponseDTO> updateAircraft(
            @PathVariable Long aircraftId,
            @Valid @RequestBody AircraftRequestDTO dto) {
        AircraftResponseDTO updated = aircraftService.updateAircraft(aircraftId, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{aircraftId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteAircraft(@PathVariable Long aircraftId) {
        aircraftService.deleteAircraft(aircraftId);
        return ResponseEntity.ok("Aircraft deactivated successfully");
    }
}
