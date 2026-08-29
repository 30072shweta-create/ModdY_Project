package com.example.flight.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flight.dto.AircraftRequestDTO;
import com.example.flight.dto.AircraftResponseDTO;
import com.example.flight.entity.Aircraft;
import com.example.flight.exception.AircraftNotFoundException;
import com.example.flight.exception.InvalidAircraftException;
import com.example.flight.repository.AircraftRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AircraftService {

    private final AircraftRepository aircraftRepository;

    @Transactional(readOnly = true)
    public List<AircraftResponseDTO> getAllAircraft() {
        return aircraftRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AircraftResponseDTO getAircraftById(Long aircraftId) {
        Aircraft aircraft = aircraftRepository.findById(aircraftId)
                .orElseThrow(() -> new AircraftNotFoundException("Aircraft not found with ID: " + aircraftId));
        return convertToResponse(aircraft);
    }

    @Transactional(readOnly = true)
    public AircraftResponseDTO getAircraftByCode(String aircraftCode) {
        Aircraft aircraft = aircraftRepository.findByAircraftCode(aircraftCode)
                .orElseThrow(() -> new AircraftNotFoundException("Aircraft not found with code: " + aircraftCode));
        return convertToResponse(aircraft);
    }

    @Transactional
    public AircraftResponseDTO createAircraft(AircraftRequestDTO dto) {
        if (aircraftRepository.existsByAircraftCode(dto.getAircraftCode())) {
            throw new InvalidAircraftException("Aircraft code already exists: " + dto.getAircraftCode());
        }

        Aircraft aircraft = Aircraft.builder()
                .aircraftCode(dto.getAircraftCode())
                .model(dto.getModel())
                .manufacturer(dto.getManufacturer())
                .totalSeatCapacity(dto.getTotalSeatCapacity())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();

        Aircraft saved = aircraftRepository.save(aircraft);
        return convertToResponse(saved);
    }

    @Transactional
    public AircraftResponseDTO updateAircraft(Long aircraftId, AircraftRequestDTO dto) {
        Aircraft aircraft = aircraftRepository.findById(aircraftId)
                .orElseThrow(() -> new AircraftNotFoundException("Aircraft not found with ID: " + aircraftId));

        if (!aircraft.getAircraftCode().equals(dto.getAircraftCode())
                && aircraftRepository.existsByAircraftCode(dto.getAircraftCode())) {
            throw new InvalidAircraftException("Aircraft code already exists: " + dto.getAircraftCode());
        }

        aircraft.setAircraftCode(dto.getAircraftCode());
        aircraft.setModel(dto.getModel());
        aircraft.setManufacturer(dto.getManufacturer());
        aircraft.setTotalSeatCapacity(dto.getTotalSeatCapacity());
        if (dto.getActive() != null) {
            aircraft.setActive(dto.getActive());
        }

        Aircraft updated = aircraftRepository.save(aircraft);
        return convertToResponse(updated);
    }

    @Transactional
    public void deleteAircraft(Long aircraftId) {
        Aircraft aircraft = aircraftRepository.findById(aircraftId)
                .orElseThrow(() -> new AircraftNotFoundException("Aircraft not found with ID: " + aircraftId));
        
        // Soft delete / deactivate
        aircraft.setActive(false);
        aircraftRepository.save(aircraft);
    }

    public AircraftResponseDTO convertToResponse(Aircraft aircraft) {
        return AircraftResponseDTO.builder()
                .aircraftId(aircraft.getAircraftId())
                .aircraftCode(aircraft.getAircraftCode())
                .model(aircraft.getModel())
                .manufacturer(aircraft.getManufacturer())
                .totalSeatCapacity(aircraft.getTotalSeatCapacity())
                .active(aircraft.getActive())
                .build();
    }
}
