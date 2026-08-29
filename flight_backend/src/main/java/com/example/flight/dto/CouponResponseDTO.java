package com.example.flight.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.flight.entity.DiscountType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponResponseDTO {

    private Long couponId;
    private String couponCode;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minimumBookingAmount;
    private BigDecimal maximumDiscount;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Integer usageLimit;
    private Integer usedCount;
    private Boolean active;
}
