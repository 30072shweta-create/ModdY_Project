package com.example.flight.service;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    @CacheEvict(value = {"airports", "airports_all"}, allEntries = true)
    public AirportResponseDTO addAirport(AirportRequestDTO dto) {
        if (airportRepository.existsById(dto.getAirportCode())) {
            throw new RuntimeException("Airport already exists with code: " + dto.getAirportCode());
        }

        Airport airport = modelMapper.map(dto, Airport.class);
        Airport savedAirport = airportRepository.save(airport);
        return modelMapper.map(savedAirport, AirportResponseDTO.class);
    }

    // ================= GET ALL AIRPORTS =================
    @Cacheable(value = "airports_all")
    public List<AirportResponseDTO> getAllAirports() {
        return airportRepository.findAll()
                .stream()
                .map(airport -> modelMapper.map(airport, AirportResponseDTO.class))
                .toList();
    }

    // ================= GET AIRPORT BY CODE =================
    @Cacheable(value = "airports", key = "#airportCode")
    public AirportResponseDTO getAirportByCode(String airportCode) {
        Airport airport = airportRepository.findById(airportCode)
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found with code: " + airportCode));

        return modelMapper.map(airport, AirportResponseDTO.class);
    }

    // ================= UPDATE AIRPORT =================
    @CacheEvict(value = {"airports", "airports_all"}, allEntries = true)
    public AirportResponseDTO updateAirport(String airportCode, AirportRequestDTO dto) {
        Airport airport = airportRepository.findById(airportCode)
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found with code: " + airportCode));

        airport.setName(dto.getName());
        airport.setCity(dto.getCity());
        airport.setCountry(dto.getCountry());

        Airport updatedAirport = airportRepository.save(airport);
        return modelMapper.map(updatedAirport, AirportResponseDTO.class);
    }

    // ================= DELETE AIRPORT =================
    @CacheEvict(value = {"airports", "airports_all"}, allEntries = true)
    public void deleteAirport(String airportCode) {
        Airport airport = airportRepository.findById(airportCode)
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found with code: " + airportCode));

        airportRepository.delete(airport);
    }
}