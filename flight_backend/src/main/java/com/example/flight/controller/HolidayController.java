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
import org.springframework.web.bind.annotation.RestController;

import com.example.flight.dto.HolidayRequestDTO;
import com.example.flight.dto.HolidayResponseDTO;
import com.example.flight.service.HolidayService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
@Tag(name = "Holidays", description = "Endpoints for managing holiday pricing calendar (ADMIN only)")
public class HolidayController {

    private final HolidayService holidayService;

    @Operation(summary = "Get all holidays")
    @GetMapping
    public List<HolidayResponseDTO> getAllHolidays() {
        return holidayService.getAllHolidays();
    }

    @Operation(summary = "Create a holiday (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public HolidayResponseDTO createHoliday(@Valid @RequestBody HolidayRequestDTO dto) {
        return holidayService.createHoliday(dto);
    }

    @Operation(summary = "Update a holiday (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{holidayId}")
    public HolidayResponseDTO updateHoliday(@PathVariable Long holidayId,
                                            @Valid @RequestBody HolidayRequestDTO dto) {
        return holidayService.updateHoliday(holidayId, dto);
    }

    @Operation(summary = "Delete a holiday (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{holidayId}")
    public void deleteHoliday(@PathVariable Long holidayId) {
        holidayService.deleteHoliday(holidayId);
    }
}
