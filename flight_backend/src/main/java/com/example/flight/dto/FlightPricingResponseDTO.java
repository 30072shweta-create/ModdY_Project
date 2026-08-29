package com.example.flight.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.flight.entity.CabinClass;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightPricingResponseDTO {

    private Long pricingId;
    private Long flightId;
    private CabinClass cabinClass;
    private BigDecimal baseFare;
    private BigDecimal tax;
    private BigDecimal airportFee;
    private BigDecimal convenienceFee;
    private BigDecimal baggageFee;
    private BigDecimal discount;
    private BigDecimal finalPrice;
    private String currency;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Backward compatibility helper
    public BigDecimal getPrice() {
        return finalPrice != null ? finalPrice : baseFare;
    }
}