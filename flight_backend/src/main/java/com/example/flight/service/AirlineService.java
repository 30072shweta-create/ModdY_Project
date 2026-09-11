package com.example.flight.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flight.dto.AirlineRequestDTO;
import com.example.flight.dto.AirlineResponseDTO;
import com.example.flight.entity.Airline;
import com.example.flight.exception.ResourceNotFoundException;
import com.example.flight.repository.AirlineRepository;

@Service
@Transactional
public class AirlineService {

    private final AirlineRepository airlineRepository;

    public AirlineService(AirlineRepository airlineRepository) {
        this.airlineRepository = airlineRepository;
    }

    // ================= ADD AIRLINE =================
    public AirlineResponseDTO addAirline(AirlineRequestDTO request) {
        String airlineCode = request.getAirlineCode().trim().toUpperCase();
        String airlineName = request.getAirlineName().trim();

        if (airlineRepository.existsById(airlineCode)) {
            throw new RuntimeException(
                    "Airline already exists with code: "
                    + airlineCode
            );
        }

        Airline airline = new Airline();
        airline.setAirlineCode(airlineCode);
        airline.setName(airlineName);

        Airline savedAirline = airlineRepository.save(airline);

        return convertToResponse(savedAirline);
    }

    // ================= GET ALL AIRLINES =================
    @Transactional(readOnly = true)
    public List<AirlineResponseDTO> getAllAirlines() {

        return airlineRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // ================= GET AIRLINE =================
    @Transactional(readOnly = true)
    public AirlineResponseDTO getAirlineByCode(String airlineCode) {

        Airline airline = airlineRepository.findById(airlineCode.trim().toUpperCase())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Airline not found with code: "
                                + airlineCode
                        ));

        return convertToResponse(airline);
    }

    // ================= UPDATE AIRLINE =================
    public AirlineResponseDTO updateAirline(
            String airlineCode,
            AirlineRequestDTO request) {

        Airline airline = airlineRepository.findById(airlineCode.trim().toUpperCase())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Airline not found with code: "
                                + airlineCode
                        ));

        airline.setName(request.getAirlineName().trim());

        Airline updatedAirline = airlineRepository.save(airline);

        return convertToResponse(updatedAirline);
    }

    // ================= DELETE AIRLINE =================
    public void deleteAirline(String airlineCode) {

        Airline airline = airlineRepository.findById(airlineCode.trim().toUpperCase())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Airline not found with code: "
                                + airlineCode
                        ));

        airlineRepository.delete(airline);
    }

    private AirlineResponseDTO convertToResponse(Airline airline) {
        return new AirlineResponseDTO(airline.getAirlineCode(), airline.getName());
    }
}
