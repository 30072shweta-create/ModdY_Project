package com.example.flight.dto;

import java.math.BigDecimal;

import com.example.flight.entity.CabinClass;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FareBreakdownDTO {

    private Long flightId;
    private CabinClass cabinClass;
    private String currency;
    private BigDecimal baseFare;
    private BigDecimal weekendAdjustment;
    private BigDecimal holidayAdjustment;
    private BigDecimal seasonalAdjustment;
    private BigDecimal promotionalDiscount;
    private BigDecimal couponDiscount;
    private BigDecimal tax;
    private BigDecimal airportFee;
    private BigDecimal convenienceFee;
    private BigDecimal baggageFee;
    private BigDecimal extraBaggageFee;
    private BigDecimal gourmetMealFee;
    private BigDecimal travelInsuranceFee;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalDiscount;
    private BigDecimal finalPrice;
    private BigDecimal totalFare;
}
