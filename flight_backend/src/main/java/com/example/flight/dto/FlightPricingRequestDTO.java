package com.example.flight.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.flight.entity.CabinClass;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;

@AllArgsConstructor
@Builder
public class FlightPricingRequestDTO {

    @NotNull(message = "Flight ID is required")
    private Long flightId;

    @NotNull(message = "Cabin class is required")
    private CabinClass cabinClass;

    @NotNull(message = "Base fare is required")
    @DecimalMin(value = "0.0", message = "Base fare cannot be negative")
    private BigDecimal baseFare;

    @DecimalMin(value = "0.0", message = "Tax cannot be negative")
    private BigDecimal tax;

    @DecimalMin(value = "0.0", message = "Taxes cannot be negative")
    private BigDecimal taxes;

    @DecimalMin(value = "0.0", message = "Airport fee cannot be negative")
    private BigDecimal airportFee;

    @DecimalMin(value = "0.0", message = "Convenience fee cannot be negative")
    private BigDecimal convenienceFee;

    @DecimalMin(value = "0.0", message = "Baggage fee cannot be negative")
    private BigDecimal baggageFee;

    @DecimalMin(value = "0.0", message = "Discount cannot be negative")
    private BigDecimal discount;

    private String currency;

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd[ 'T'HH:mm[:ss]]")
    private LocalDateTime effectiveFrom;

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd[ 'T'HH:mm[:ss]]")
    private LocalDateTime effectiveTo;

    public FlightPricingRequestDTO() {}

    public Long getFlightId() { return flightId; }
    public void setFlightId(Long flightId) { this.flightId = flightId; }

    public CabinClass getCabinClass() { return cabinClass; }
    public void setCabinClass(CabinClass cabinClass) { this.cabinClass = cabinClass; }

    public BigDecimal getBaseFare() { return baseFare; }
    public void setBaseFare(BigDecimal baseFare) { this.baseFare = baseFare; }

    public BigDecimal getTax() { return tax != null ? tax : taxes; }
    public void setTax(BigDecimal tax) { 
        this.tax = tax; 
        if (this.taxes == null) this.taxes = tax;
    }

    public BigDecimal getTaxes() { return taxes != null ? taxes : tax; }
    public void setTaxes(BigDecimal taxes) { 
        this.taxes = taxes; 
        if (this.tax == null) this.tax = taxes;
    }

    public BigDecimal getAirportFee() { return airportFee; }
    public void setAirportFee(BigDecimal airportFee) { this.airportFee = airportFee; }

    public BigDecimal getConvenienceFee() { return convenienceFee; }
    public void setConvenienceFee(BigDecimal convenienceFee) { this.convenienceFee = convenienceFee; }

    public BigDecimal getBaggageFee() { return baggageFee; }
    public void setBaggageFee(BigDecimal baggageFee) { this.baggageFee = baggageFee; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDateTime getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDateTime effectiveTo) { this.effectiveTo = effectiveTo; }
}