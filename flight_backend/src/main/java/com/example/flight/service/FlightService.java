package com.example.flight.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flight.dto.FlightRequestDTO;
import com.example.flight.dto.FlightResponseDTO;
import com.example.flight.dto.FlightSearchRequestDTO;
import com.example.flight.dto.FlightStatusRequestDTO;
import com.example.flight.entity.Aircraft;
import com.example.flight.entity.Airline;
import com.example.flight.entity.Airport;
import com.example.flight.entity.CabinClass;
import com.example.flight.entity.Flight;
import com.example.flight.entity.FlightPricing;
import com.example.flight.entity.FlightStatus;
import com.example.flight.exception.AircraftNotFoundException;
import com.example.flight.exception.InvalidAircraftException;
import com.example.flight.repository.AircraftRepository;
import com.example.flight.repository.AirlineRepository;
import com.example.flight.repository.AirportRepository;
import com.example.flight.repository.FlightPricingRepository;
import com.example.flight.repository.FlightRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightRepository flightRepository;
    private final AirlineRepository airlineRepository;
    private final AirportRepository airportRepository;
    private final AircraftRepository aircraftRepository;
    private final FlightPricingRepository flightPricingRepository;
    private final ModelMapper modelMapper;

    // Get All Flights
    @Transactional(readOnly = true)
    public List<FlightResponseDTO> getAllFlights() {
        return flightRepository.findAllWithDetails()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // Get Flight By Id
    @Transactional(readOnly = true)
    public FlightResponseDTO getFlightById(Long flightId) {
        Flight flight = flightRepository.findByIdWithDetails(flightId)
                .orElseThrow(() -> new RuntimeException("Flight not found with ID: " + flightId));

        return convertToResponse(flight);
    }

    // Track Flight By Flight Number
    @Transactional(readOnly = true)
    public FlightResponseDTO trackFlight(String flightNumber) {
        if (flightNumber == null || flightNumber.isBlank()) {
            throw new RuntimeException("Flight number is required for tracking");
        }
        return flightRepository.findByFlightNumberWithDetails(flightNumber.trim())
                .map(this::convertToResponse)
                .orElseThrow(() -> new RuntimeException("Flight not found with flight number: " + flightNumber.trim()));
    }

    // Search Flights
    @Transactional(readOnly = true)
    public Page<FlightResponseDTO> searchFlights(FlightSearchRequestDTO request) {
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;

        if (request.getDate() != null) {
            startDateTime = request.getDate().atStartOfDay();
            endDateTime = request.getDate().plusDays(1).atStartOfDay();
        } else {
            startDateTime = LocalDateTime.of(2000, 1, 1, 0, 0);
            endDateTime = LocalDateTime.of(2099, 12, 31, 23, 59);
        }

        String sortField = getSortField(request.getSortBy());
        Sort.Direction direction = "desc".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(direction, sortField)
        );

        return flightRepository.searchFlights(
                request.getSource(),
                request.getDestination(),
                request.getDate(),
                startDateTime,
                endDateTime,
                request.getAirline(),
                request.getFlightNumber(),
                request.getStops(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getMaxDuration(),
                pageable
        ).map(this::convertToResponse);
    }

    // Add Flight
    @Transactional
    public FlightResponseDTO addFlight(FlightRequestDTO dto) {
        String flightNumber = dto.getFlightNumber().trim().toUpperCase();
        String airlineCode = dto.getAirlineCode().trim().toUpperCase();
        String fromAirportCode = dto.getFromAirport().trim().toUpperCase();
        String toAirportCode = dto.getToAirport().trim().toUpperCase();

        if (flightRepository.existsByFlightNumberIgnoreCase(flightNumber)) {
            throw new RuntimeException("Flight already exists with number: " + flightNumber);
        }

        Airline airline = airlineRepository.findById(airlineCode)
                .orElseThrow(() -> new RuntimeException("Airline not found"));

        Airport sourceAirport = airportRepository.findById(fromAirportCode)
                .orElseThrow(() -> new RuntimeException("Source Airport not found"));

        Airport destinationAirport = airportRepository.findById(toAirportCode)
                .orElseThrow(() -> new RuntimeException("Destination Airport not found"));

        Aircraft aircraft = null;
        if (dto.getAircraftId() != null) {
            aircraft = aircraftRepository.findById(dto.getAircraftId())
                    .orElseThrow(() -> new AircraftNotFoundException("Aircraft not found with ID: " + dto.getAircraftId()));

            if (Boolean.FALSE.equals(aircraft.getActive())) {
                throw new InvalidAircraftException("Cannot assign inactive aircraft ID: " + dto.getAircraftId());
            }
        }

        Flight flight = modelMapper.map(dto, Flight.class);
        flight.setFlightId(null);
        flight.setFlightNumber(flightNumber);
        flight.setAirline(airline);
        flight.setFromAirport(sourceAirport);
        flight.setToAirport(destinationAirport);
        flight.setAircraft(aircraft);
        flight.setStatus(dto.getStatus() != null ? dto.getStatus() : FlightStatus.SCHEDULED);

        if (aircraft != null && (flight.getAvailableSeats() == null || flight.getAvailableSeats() <= 0)) {
            flight.setAvailableSeats(aircraft.getTotalSeatCapacity().shortValue());
        }

        Flight savedFlight = flightRepository.save(flight);
        ensureEconomyPricing(savedFlight);
        return convertToResponse(savedFlight);
    }

    // Update Flight
    @Transactional
    public FlightResponseDTO updateFlight(Long flightId, FlightRequestDTO dto) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new RuntimeException("Flight not found"));

        String flightNumber = dto.getFlightNumber().trim().toUpperCase();
        String airlineCode = dto.getAirlineCode().trim().toUpperCase();
        String fromAirportCode = dto.getFromAirport().trim().toUpperCase();
        String toAirportCode = dto.getToAirport().trim().toUpperCase();

        Airline airline = airlineRepository.findById(airlineCode)
                .orElseThrow(() -> new RuntimeException("Airline not found"));

        Airport sourceAirport = airportRepository.findById(fromAirportCode)
                .orElseThrow(() -> new RuntimeException("Source Airport not found"));

        Airport destinationAirport = airportRepository.findById(toAirportCode)
                .orElseThrow(() -> new RuntimeException("Destination Airport not found"));

        Aircraft aircraft = null;
        if (dto.getAircraftId() != null) {
            aircraft = aircraftRepository.findById(dto.getAircraftId())
                    .orElseThrow(() -> new AircraftNotFoundException("Aircraft not found with ID: " + dto.getAircraftId()));

            if (Boolean.FALSE.equals(aircraft.getActive())) {
                throw new InvalidAircraftException("Cannot assign inactive aircraft ID: " + dto.getAircraftId());
            }
        }

        flight.setFlightNumber(flightNumber);
        if (dto.getDepartureTs() != null) flight.setDepartureTs(dto.getDepartureTs());
        if (dto.getArrivalTs() != null) flight.setArrivalTs(dto.getArrivalTs());
        if (dto.getStops() != null) flight.setStops(dto.getStops());
        if (dto.getBasePrice() != null) flight.setBasePrice(dto.getBasePrice());
        if (dto.getAvailableSeats() != null) flight.setAvailableSeats(dto.getAvailableSeats());
        if (dto.getDurationMins() != null) flight.setDurationMins(dto.getDurationMins());
        if (dto.getStatus() != null) flight.setStatus(dto.getStatus());

        flight.setAirline(airline);
        flight.setFromAirport(sourceAirport);
        flight.setToAirport(destinationAirport);
        flight.setAircraft(aircraft);

        Flight updatedFlight = flightRepository.save(flight);
        ensureEconomyPricing(updatedFlight);
        return convertToResponse(updatedFlight);
    }

    // Delete Flight
    @Transactional
    public void deleteFlight(Long flightId) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new RuntimeException("Flight not found"));
        flightRepository.delete(flight);
    }

    // Convert Entity to Response DTO
    public FlightResponseDTO convertToResponse(Flight flight) {
        FlightResponseDTO response = modelMapper.map(flight, FlightResponseDTO.class);

        if (flight.getAirline() != null) {
            response.setAirlineCode(flight.getAirline().getAirlineCode());
            response.setAirlineName(flight.getAirline().getName());
        }

        if (flight.getFromAirport() != null) {
            response.setFromAirport(flight.getFromAirport().getAirportCode());
        }

        if (flight.getToAirport() != null) {
            response.setToAirport(flight.getToAirport().getAirportCode());
        }

        if (flight.getAircraft() != null) {
            response.setAircraftId(flight.getAircraft().getAircraftId());
            response.setAircraftCode(flight.getAircraft().getAircraftCode());
            response.setAircraftModel(flight.getAircraft().getModel());
            response.setTotalSeatCapacity(flight.getAircraft().getTotalSeatCapacity());
        }

        return response;
    }

    private String getSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "departureTs";
        }

        return switch (sortBy.toLowerCase()) {
            case "price" -> "basePrice";
            case "departuretime" -> "departureTs";
            case "arrivaltime" -> "arrivalTs";
            case "duration" -> "durationMins";
            default -> "departureTs";
        };
    }

    private void ensureEconomyPricing(Flight flight) {
        if (flight.getFlightId() == null) {
            return;
        }

        boolean hasEconomyPricing = flightPricingRepository
                .findByFlightFlightIdAndSeatClass(flight.getFlightId(), CabinClass.ECONOMY)
                .isPresent();

        if (hasEconomyPricing) {
            return;
        }

        LocalDateTime effectiveFrom = flight.getDepartureTs() != null
                ? flight.getDepartureTs().minusDays(1)
                : LocalDateTime.now();

        FlightPricing pricing = FlightPricing.builder()
                .flight(flight)
                .seatClass(CabinClass.ECONOMY)
                .baseFare(flight.getBasePrice() != null ? flight.getBasePrice() : BigDecimal.ZERO)
                .tax(BigDecimal.ZERO)
                .taxes(BigDecimal.ZERO)
                .airportFee(BigDecimal.ZERO)
                .convenienceFee(BigDecimal.ZERO)
                .baggageFee(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .currency("INR")
                .effectiveFrom(effectiveFrom)
                .effectiveTo(null)
                .build();

        pricing.calculateFinalPrice();
        flightPricingRepository.save(pricing);
    }

    // Update Flight Status (Admin)
    @Transactional
    public FlightResponseDTO updateFlightStatus(Long flightId, FlightStatusRequestDTO request) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new RuntimeException("Flight not found"));

        flight.setStatus(request.getStatus());
        Flight updatedFlight = flightRepository.save(flight);

        return convertToResponse(updatedFlight);
    }
}
