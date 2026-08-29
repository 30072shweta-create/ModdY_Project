package com.example.flight.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flight.dto.HolidayRequestDTO;
import com.example.flight.dto.HolidayResponseDTO;
import com.example.flight.entity.Holiday;
import com.example.flight.repository.HolidayRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayRepository holidayRepository;

    @Transactional(readOnly = true)
    public List<HolidayResponseDTO> getAllHolidays() {
        return holidayRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public HolidayResponseDTO createHoliday(HolidayRequestDTO dto) {
        if (holidayRepository.existsByHolidayDate(dto.getHolidayDate())) {
            throw new RuntimeException("Holiday already exists on date: " + dto.getHolidayDate());
        }

        Holiday holiday = Holiday.builder()
                .name(dto.getName())
                .holidayDate(dto.getHolidayDate())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();

        Holiday saved = holidayRepository.save(holiday);
        return convertToResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public HolidayResponseDTO updateHoliday(Long holidayId, HolidayRequestDTO dto) {
        Holiday holiday = holidayRepository.findById(holidayId)
                .orElseThrow(() -> new RuntimeException("Holiday not found with ID: " + holidayId));

        holiday.setName(dto.getName());
        holiday.setHolidayDate(dto.getHolidayDate());
        if (dto.getActive() != null) holiday.setActive(dto.getActive());

        Holiday saved = holidayRepository.save(holiday);
        return convertToResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteHoliday(Long holidayId) {
        Holiday holiday = holidayRepository.findById(holidayId)
                .orElseThrow(() -> new RuntimeException("Holiday not found with ID: " + holidayId));
        holidayRepository.delete(holiday);
    }

    public HolidayResponseDTO convertToResponse(Holiday holiday) {
        return HolidayResponseDTO.builder()
                .holidayId(holiday.getHolidayId())
                .name(holiday.getName())
                .holidayDate(holiday.getHolidayDate())
                .active(holiday.getActive())
                .build();
    }
}
