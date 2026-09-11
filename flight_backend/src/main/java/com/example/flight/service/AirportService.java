package com.example.flight.service;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.example.flight.dto.AirportRequestDTO;
import com.example.flight.dto.AirportResponseDTO;
import com.example.flight.entity.Airport;
import com.example.flight.exception.ResourceNotFoundException;
import com.example.flight.repository.AirportRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AirportService {

    private final AirportRepository airportRepository;
    private final ModelMapper modelMapper;

    // ================= ADD AIRPORT =================
    public AirportResponseDTO addAirport(AirportRequestDTO dto) {
        String airportCode = dto.getAirportCode().trim().toUpperCase();

        if (airportRepository.existsById(airportCode)) {
            throw new RuntimeException("Airport already exists with code: " + airportCode);
        }

        Airport airport = new Airport();
        airport.setAirportCode(airportCode);
        airport.setName(dto.getName().trim());
        airport.setCity(dto.getCity().trim());
        airport.setCountry(dto.getCountry().trim());

        Airport savedAirport = airportRepository.save(airport);
        return modelMapper.map(savedAirport, AirportResponseDTO.class);
    }

    // ================= GET ALL AIRPORTS =================
    public List<AirportResponseDTO> getAllAirports() {
        return airportRepository.findAll()
                .stream()
                .map(airport -> modelMapper.map(airport, AirportResponseDTO.class))
                .toList();
    }

    // ================= GET AIRPORT BY CODE =================
    public AirportResponseDTO getAirportByCode(String airportCode) {
        String normalizedAirportCode = airportCode.trim().toUpperCase();
        Airport airport = airportRepository.findById(normalizedAirportCode)
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found with code: " + normalizedAirportCode));

        return modelMapper.map(airport, AirportResponseDTO.class);
    }

    // ================= UPDATE AIRPORT =================
    public AirportResponseDTO updateAirport(String airportCode, AirportRequestDTO dto) {
        String normalizedAirportCode = airportCode.trim().toUpperCase();
        Airport airport = airportRepository.findById(normalizedAirportCode)
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found with code: " + normalizedAirportCode));

        airport.setName(dto.getName().trim());
        airport.setCity(dto.getCity().trim());
        airport.setCountry(dto.getCountry().trim());

        Airport updatedAirport = airportRepository.save(airport);
        return modelMapper.map(updatedAirport, AirportResponseDTO.class);
    }

    // ================= DELETE AIRPORT =================
    public void deleteAirport(String airportCode) {
        String normalizedAirportCode = airportCode.trim().toUpperCase();
        Airport airport = airportRepository.findById(normalizedAirportCode)
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found with code: " + normalizedAirportCode));

        airportRepository.delete(airport);
    }
}
