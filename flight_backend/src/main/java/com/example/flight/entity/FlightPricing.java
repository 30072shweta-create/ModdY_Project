package com.example.flight.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "flight_pricing",
        schema = "flight_booking_system",
        indexes = {
            @Index(name = "idx_fp_flight_class_eff", columnList = "flight_id, seat_class, effective_from, effective_to")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightPricing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pricing_id")
    private Long pricingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_class", nullable = false, length = 30)
    private CabinClass seatClass;

    @Column(name = "base_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseFare;

    @Column(name = "tax", nullable = false, precision = 12, scale = 2)
    private BigDecimal tax;

    @Column(name = "taxes", precision = 12, scale = 2)
    private BigDecimal taxes;

    @Column(name = "airport_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal airportFee;

    @Column(name = "convenience_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal convenienceFee;

    @Column(name = "baggage_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal baggageFee;

    @Column(name = "discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discount;

    @Column(name = "final_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalPrice;

    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
        if (tax == null && taxes != null) tax = taxes;
        if (taxes == null && tax != null) taxes = tax;
        if (tax == null) tax = BigDecimal.ZERO;
        if (taxes == null) taxes = BigDecimal.ZERO;
        if (airportFee == null) airportFee = BigDecimal.ZERO;
        if (convenienceFee == null) convenienceFee = BigDecimal.ZERO;
        if (baggageFee == null) baggageFee = BigDecimal.ZERO;
        if (discount == null) discount = BigDecimal.ZERO;
        if (effectiveFrom == null) effectiveFrom = LocalDateTime.now();
        calculateFinalPrice();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (tax == null && taxes != null) tax = taxes;
        if (taxes == null && tax != null) taxes = tax;
        if (tax == null) tax = BigDecimal.ZERO;
        if (taxes == null) taxes = BigDecimal.ZERO;
        calculateFinalPrice();
    }

    public void calculateFinalPrice() {
        BigDecimal base = baseFare != null ? baseFare : BigDecimal.ZERO;
        BigDecimal tx = getTax();
        BigDecimal ap = airportFee != null ? airportFee : BigDecimal.ZERO;
        BigDecimal cf = convenienceFee != null ? convenienceFee : BigDecimal.ZERO;
        BigDecimal bg = baggageFee != null ? baggageFee : BigDecimal.ZERO;
        BigDecimal dc = discount != null ? discount : BigDecimal.ZERO;

        BigDecimal total = base.add(tx).add(ap).add(cf).add(bg).subtract(dc);
        this.finalPrice = total.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : total;
    }

    public Long getPricingId() { return pricingId; }
    public void setPricingId(Long pricingId) { this.pricingId = pricingId; }

    public Flight getFlight() { return flight; }
    public void setFlight(Flight flight) { this.flight = flight; }

    public CabinClass getSeatClass() { return seatClass; }
    public void setSeatClass(CabinClass seatClass) { this.seatClass = seatClass; }

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

    public BigDecimal getFinalPrice() { return finalPrice; }
    public void setFinalPrice(BigDecimal finalPrice) { this.finalPrice = finalPrice; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDateTime getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDateTime effectiveTo) { this.effectiveTo = effectiveTo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}