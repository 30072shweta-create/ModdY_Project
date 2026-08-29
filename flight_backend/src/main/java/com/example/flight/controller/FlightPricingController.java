package com.example.flight.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.flight.dto.FareBreakdownDTO;
import com.example.flight.dto.FareCalculationRequestDTO;
import com.example.flight.dto.FlightPricingRequestDTO;
import com.example.flight.dto.FlightPricingResponseDTO;
import com.example.flight.entity.CabinClass;
import com.example.flight.service.FareCalculationService;
import com.example.flight.service.FlightPricingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/flight-pricing")
@RequiredArgsConstructor
@Tag(name = "Flight Pricing", description = "Endpoints for managing flight pricing, historical fare tracking, and fare calculations")
public class FlightPricingController {

    private final FlightPricingService flightPricingService;
    private final FareCalculationService fareCalculationService;

    @Operation(summary = "Get all flight pricing records")
    @GetMapping
    public List<FlightPricingResponseDTO> getAllPricing() {
        return flightPricingService.getAllPricing();
    }

    @Operation(summary = "Get flight pricing record by ID")
    @GetMapping("/{pricingId}")
    public FlightPricingResponseDTO getPricingById(@PathVariable Long pricingId) {
        return flightPricingService.getPricingById(pricingId);
    }

    @Operation(summary = "Get all pricing records for a specific flight")
    @GetMapping("/flight/{flightId}")
    public List<FlightPricingResponseDTO> getPricingByFlightId(@PathVariable Long flightId) {
        return flightPricingService.getPricingByFlightId(flightId);
    }

    @Operation(summary = "Get pricing history for a specific flight and cabin class")
    @GetMapping("/flight/{flightId}/history")
    public List<FlightPricingResponseDTO> getPricingHistory(
            @PathVariable Long flightId,
            @RequestParam(required = false, defaultValue = "ECONOMY") CabinClass cabinClass) {
        return flightPricingService.getPricingHistory(flightId, cabinClass);
    }

    @Operation(summary = "Calculate final fare with dynamic rules, taxes, fees, and coupons")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Fare calculated successfully"),
        @ApiResponse(responseCode = "404", description = "Flight or pricing not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request or coupon")
    })
    @PostMapping("/calculate")
    public FareBreakdownDTO calculateFare(@Valid @RequestBody FareCalculationRequestDTO request) {
        return fareCalculationService.calculateFare(request);
    }

    @Operation(summary = "Add new flight pricing record (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public FlightPricingResponseDTO addPricing(@Valid @RequestBody FlightPricingRequestDTO dto) {
        return flightPricingService.addPricing(dto);
    }

    @Operation(summary = "Update flight pricing record (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{pricingId}")
    public FlightPricingResponseDTO updatePricing(@PathVariable Long pricingId,
                                                  @Valid @RequestBody FlightPricingRequestDTO dto) {
        return flightPricingService.updatePricing(pricingId, dto);
    }

    @Operation(summary = "Delete flight pricing record (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{pricingId}")
    public void deletePricing(@PathVariable Long pricingId) {
        flightPricingService.deletePricing(pricingId);
    }
}
