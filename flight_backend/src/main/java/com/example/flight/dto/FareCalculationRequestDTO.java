package com.example.flight.dto;

import java.time.LocalDateTime;

import com.example.flight.entity.CabinClass;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FareCalculationRequestDTO {

    @NotNull(message = "Flight ID is required")
    private Long flightId;

    private Long bookingId;

    @NotNull(message = "Cabin class is required")
    private CabinClass cabinClass;

    private LocalDateTime travelDate;

    private Integer passengerCount;

    private Integer baggageCount;

    private Integer baggageKg;

    private Boolean extraBaggage;

    private Boolean gourmetMeal;

    private Boolean travelInsurance;

    private String couponCode;
}
