package com.example.flight.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flight.dto.FlightPricingRequestDTO;
import com.example.flight.dto.FlightPricingResponseDTO;
import com.example.flight.entity.CabinClass;
import com.example.flight.entity.Flight;
import com.example.flight.entity.FlightPricing;
import com.example.flight.exception.FlightNotFoundException;
import com.example.flight.exception.InvalidPricingPeriodException;
import com.example.flight.exception.PricingNotFoundException;
import com.example.flight.repository.FlightPricingRepository;
import com.example.flight.repository.FlightRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FlightPricingService {

    private final FlightPricingRepository flightPricingRepository;
    private final FlightRepository flightRepository;

    @Transactional(readOnly = true)
    public List<FlightPricingResponseDTO> getAllPricing() {
        return flightPricingRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FlightPricingResponseDTO getPricingById(Long pricingId) {
        FlightPricing pricing = flightPricingRepository.findById(pricingId)
                .orElseThrow(() -> new PricingNotFoundException("Flight pricing not found with ID: " + pricingId));
        return convertToResponse(pricing);
    }

    @Transactional(readOnly = true)
    public List<FlightPricingResponseDTO> getPricingByFlightId(Long flightId) {
        if (!flightRepository.existsById(flightId)) {
            throw new FlightNotFoundException("Flight not found with ID: " + flightId);
        }
        return flightPricingRepository.findByFlightFlightIdOrderByEffectiveFromDesc(flightId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Cacheable(value = "flightPricing", key = "#flightId + '-' + #seatClass")
    @Transactional(readOnly = true)
    public FlightPricingResponseDTO getActivePricing(Long flightId, CabinClass seatClass) {
        FlightPricing pricing = flightPricingRepository.findActivePricing(flightId, seatClass, LocalDateTime.now())
                .orElseGet(() -> flightPricingRepository.findByFlightFlightIdAndSeatClass(flightId, seatClass)
                        .orElseThrow(() -> new PricingNotFoundException("Active pricing not found for flight: " + flightId + " class: " + seatClass)));
        return convertToResponse(pricing);
    }

    @Transactional(readOnly = true)
    public List<FlightPricingResponseDTO> getPricingHistory(Long flightId, CabinClass seatClass) {
        return flightPricingRepository.findByFlightFlightIdAndSeatClassOrderByEffectiveFromDesc(flightId, seatClass)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @CacheEvict(value = "flightPricing", allEntries = true)
    @Transactional
    public FlightPricingResponseDTO addPricing(FlightPricingRequestDTO request) {
        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new FlightNotFoundException("Flight not found with ID: " + request.getFlightId()));

        LocalDateTime newEffectiveFrom = request.getEffectiveFrom() != null ? request.getEffectiveFrom() : LocalDateTime.now();

        if (request.getEffectiveTo() != null && request.getEffectiveTo().isBefore(newEffectiveFrom)) {
            throw new InvalidPricingPeriodException("effectiveTo cannot be before effectiveFrom");
        }

        // Close previous active pricing record if exists
        List<FlightPricing> activeRecords = flightPricingRepository.findActivePricingList(flight.getFlightId(), request.getCabinClass(), newEffectiveFrom);
        for (FlightPricing oldPricing : activeRecords) {
            oldPricing.setEffectiveTo(newEffectiveFrom);
            flightPricingRepository.save(oldPricing);
        }

        BigDecimal taxVal = request.getTax() != null ? request.getTax() : (request.getTaxes() != null ? request.getTaxes() : java.math.BigDecimal.ZERO);

        FlightPricing pricing = FlightPricing.builder()
                .flight(flight)
                .seatClass(request.getCabinClass())
                .baseFare(request.getBaseFare())
                .tax(taxVal)
                .taxes(taxVal)
                .airportFee(request.getAirportFee() != null ? request.getAirportFee() : java.math.BigDecimal.ZERO)
                .convenienceFee(request.getConvenienceFee() != null ? request.getConvenienceFee() : java.math.BigDecimal.ZERO)
                .baggageFee(request.getBaggageFee() != null ? request.getBaggageFee() : java.math.BigDecimal.ZERO)
                .discount(request.getDiscount() != null ? request.getDiscount() : java.math.BigDecimal.ZERO)
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .effectiveFrom(newEffectiveFrom)
                .effectiveTo(request.getEffectiveTo())
                .build();

        pricing.calculateFinalPrice();
        FlightPricing saved = flightPricingRepository.save(pricing);
        return convertToResponse(saved);
    }

    @CacheEvict(value = "flightPricing", allEntries = true)
    @Transactional
    public FlightPricingResponseDTO updatePricing(Long pricingId, FlightPricingRequestDTO dto) {
        FlightPricing pricing = flightPricingRepository.findById(pricingId)
                .orElseThrow(() -> new PricingNotFoundException("Flight pricing not found with ID: " + pricingId));

        if (dto.getBaseFare() != null) pricing.setBaseFare(dto.getBaseFare());
        BigDecimal updateTax = dto.getTax() != null ? dto.getTax() : dto.getTaxes();
        if (updateTax != null) {
            pricing.setTax(updateTax);
            pricing.setTaxes(updateTax);
        }
        if (dto.getAirportFee() != null) pricing.setAirportFee(dto.getAirportFee());
        if (dto.getConvenienceFee() != null) pricing.setConvenienceFee(dto.getConvenienceFee());
        if (dto.getBaggageFee() != null) pricing.setBaggageFee(dto.getBaggageFee());
        if (dto.getDiscount() != null) pricing.setDiscount(dto.getDiscount());
        if (dto.getCurrency() != null) pricing.setCurrency(dto.getCurrency());
        if (dto.getEffectiveFrom() != null) pricing.setEffectiveFrom(dto.getEffectiveFrom());
        if (dto.getEffectiveTo() != null) pricing.setEffectiveTo(dto.getEffectiveTo());

        pricing.calculateFinalPrice();
        FlightPricing updated = flightPricingRepository.save(pricing);
        return convertToResponse(updated);
    }

    @CacheEvict(value = "flightPricing", allEntries = true)
    @Transactional
    public void deletePricing(Long pricingId) {
        FlightPricing pricing = flightPricingRepository.findById(pricingId)
                .orElseThrow(() -> new PricingNotFoundException("Flight pricing not found with ID: " + pricingId));
        flightPricingRepository.delete(pricing);
    }

    public FlightPricingResponseDTO convertToResponse(FlightPricing pricing) {
        return FlightPricingResponseDTO.builder()
                .pricingId(pricing.getPricingId())
                .flightId(pricing.getFlight() != null ? pricing.getFlight().getFlightId() : null)
                .cabinClass(pricing.getSeatClass())
                .baseFare(pricing.getBaseFare())
                .tax(pricing.getTax())
                .airportFee(pricing.getAirportFee())
                .convenienceFee(pricing.getConvenienceFee())
                .baggageFee(pricing.getBaggageFee())
                .discount(pricing.getDiscount())
                .finalPrice(pricing.getFinalPrice())
                .currency(pricing.getCurrency())
                .effectiveFrom(pricing.getEffectiveFrom())
                .effectiveTo(pricing.getEffectiveTo())
                .createdAt(pricing.getCreatedAt())
                .updatedAt(pricing.getUpdatedAt())
                .build();
    }
}