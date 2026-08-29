package com.example.flight.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.flight.entity.DiscountType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponRequestDTO {

    @NotBlank(message = "Coupon code is required")
    private String couponCode;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.0", message = "Discount value cannot be negative")
    private BigDecimal discountValue;

    private BigDecimal minimumBookingAmount;
    private BigDecimal maximumDiscount;

    private LocalDateTime validFrom;
    private LocalDateTime validTo;

    private Integer usageLimit;
    private Boolean active;
}
