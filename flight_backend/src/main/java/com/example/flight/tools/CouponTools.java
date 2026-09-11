package com.example.flight.tools;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.example.flight.dto.CouponResponseDTO;
import com.example.flight.service.CouponService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CouponTools {

    private final CouponService couponService;

    @Tool(description = """
            Get all currently active coupons and promo discount codes available in the system.
            Use this tool when the user asks what coupons, promo codes, or discounts are currently active or available.
            No parameters required.
            """)
    public List<CouponResponseDTO> getActiveCoupons() {
        System.out.println("ACTIVE COUPONS TOOL CALLED");
        return couponService.getActiveCoupons();
    }

    @Tool(description = """
            Validate a coupon code and calculate discount for a booking amount.
            Use this tool when the user asks whether a coupon is valid, whether it can be applied,
            or asks about coupon discount or conditions.
            Coupon code is required. Booking amount is optional (default 0).
            """)
    public Object validateCoupon(
            String couponCode,
            BigDecimal bookingAmount) {

        System.out.println("COUPON VALIDATE TOOL CALLED FOR: " + couponCode + " with amount: " + bookingAmount);
        if (couponCode == null || couponCode.isBlank()) {
            return "Please specify a coupon code to validate.";
        }
        String cleanCode = couponCode.trim().replaceAll("[\"']", "");
        BigDecimal amount = bookingAmount != null ? bookingAmount : BigDecimal.ZERO;
        try {
            CouponResponseDTO dto = couponService.validateAndGetCoupon(cleanCode, amount);
            BigDecimal discount = couponService.calculateCouponDiscount(cleanCode, amount);
            return "Coupon " + dto.getCouponCode() + " is valid! Discount type: " + dto.getDiscountType()
                    + ", Discount value: " + dto.getDiscountValue()
                    + (amount.compareTo(BigDecimal.ZERO) > 0 ? " (Calculated discount on ₹" + amount + ": ₹" + discount + ")" : "")
                    + (dto.getMinimumBookingAmount() != null ? ", Min booking amount: ₹" + dto.getMinimumBookingAmount() : "");
        } catch (Exception e) {
            return "Coupon '" + cleanCode + "' is not valid: " + e.getMessage();
        }
    }
}