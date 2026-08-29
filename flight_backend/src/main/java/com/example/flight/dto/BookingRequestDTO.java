package com.example.flight.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.flight.entity.CabinClass;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequestDTO {

    @NotEmpty(message = "At least one flight is required")
    private List<Long> flightIds;

    @NotNull(message = "Cabin class is required")
    private CabinClass cabinClass;

    private String couponCode;

    private Integer baggageCount;

    private Integer baggageKg;

    private LocalDateTime travelDate;
}