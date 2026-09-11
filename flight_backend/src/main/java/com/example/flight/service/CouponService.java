package com.example.flight.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flight.dto.CouponRequestDTO;
import com.example.flight.dto.CouponResponseDTO;
import com.example.flight.entity.Coupon;
import com.example.flight.entity.DiscountType;
import com.example.flight.exception.CouponExpiredException;
import com.example.flight.exception.CouponUsageLimitExceededException;
import com.example.flight.exception.InvalidCouponException;
import com.example.flight.repository.CouponRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional(readOnly = true)
    public CouponResponseDTO validateAndGetCoupon(String couponCode, BigDecimal bookingAmount) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            throw new InvalidCouponException("Coupon code is required");
        }
        BigDecimal amount = bookingAmount != null ? bookingAmount : BigDecimal.ZERO;
        calculateCouponDiscount(couponCode, amount);

        Coupon coupon = couponRepository.findByCouponCodeIgnoreCase(couponCode.trim())
                .orElseThrow(() -> new InvalidCouponException("Coupon code not found: " + couponCode));

        return convertToResponse(coupon);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateCouponDiscount(String couponCode, BigDecimal bookingAmount) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        Coupon coupon = couponRepository.findByCouponCodeIgnoreCase(couponCode.trim())
                .orElseThrow(() -> new InvalidCouponException("Coupon code not found: " + couponCode));

        if (Boolean.FALSE.equals(coupon.getActive())) {
            throw new InvalidCouponException("Coupon is inactive: " + couponCode);
        }

        LocalDateTime now = LocalDateTime.now();
        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
            throw new CouponExpiredException("Coupon is not valid yet: " + couponCode);
        }
        if (coupon.getValidTo() != null && now.isAfter(coupon.getValidTo())) {
            throw new CouponExpiredException("Coupon has expired: " + couponCode);
        }

        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new CouponUsageLimitExceededException("Coupon usage limit exceeded for code: " + couponCode);
        }

        if (coupon.getMinimumBookingAmount() != null && bookingAmount.compareTo(coupon.getMinimumBookingAmount()) < 0) {
            throw new InvalidCouponException("Minimum booking amount of " + coupon.getMinimumBookingAmount() + " required for coupon: " + couponCode);
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = bookingAmount.multiply(coupon.getDiscountValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else if (coupon.getDiscountType() == DiscountType.FIXED) {
            discount = coupon.getDiscountValue();
        }

        if (coupon.getMaximumDiscount() != null && discount.compareTo(coupon.getMaximumDiscount()) > 0) {
            discount = coupon.getMaximumDiscount();
        }

        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public void incrementCouponUsage(String couponCode) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            return;
        }
        Coupon coupon = couponRepository.findByCouponCodeIgnoreCase(couponCode.trim()).orElse(null);
        if (coupon != null) {
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CouponResponseDTO createCoupon(CouponRequestDTO dto) {
        if (couponRepository.existsByCouponCodeIgnoreCase(dto.getCouponCode())) {
            throw new InvalidCouponException("Coupon code already exists: " + dto.getCouponCode());
        }

        Coupon coupon = Coupon.builder()
                .couponCode(dto.getCouponCode().toUpperCase())
                .discountType(dto.getDiscountType())
                .discountValue(dto.getDiscountValue())
                .minimumBookingAmount(dto.getMinimumBookingAmount())
                .maximumDiscount(dto.getMaximumDiscount())
                .validFrom(dto.getValidFrom())
                .validTo(dto.getValidTo())
                .usageLimit(dto.getUsageLimit())
                .usedCount(0)
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();

        Coupon saved = couponRepository.save(coupon);
        return convertToResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CouponResponseDTO updateCoupon(Long couponId, CouponRequestDTO dto) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new InvalidCouponException("Coupon not found with ID: " + couponId));

        coupon.setDiscountType(dto.getDiscountType());
        coupon.setDiscountValue(dto.getDiscountValue());
        coupon.setMinimumBookingAmount(dto.getMinimumBookingAmount());
        coupon.setMaximumDiscount(dto.getMaximumDiscount());
        coupon.setValidFrom(dto.getValidFrom());
        coupon.setValidTo(dto.getValidTo());
        coupon.setUsageLimit(dto.getUsageLimit());
        if (dto.getActive() != null) {
            coupon.setActive(dto.getActive());
        }

        Coupon saved = couponRepository.save(coupon);
        return convertToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CouponResponseDTO> getActiveCoupons() {
        LocalDateTime now = LocalDateTime.now();
        return couponRepository.findByActiveTrue().stream()
                .filter(c -> (c.getValidFrom() == null || !now.isBefore(c.getValidFrom()))
                        && (c.getValidTo() == null || !now.isAfter(c.getValidTo()))
                        && (c.getUsageLimit() == null || c.getUsedCount() < c.getUsageLimit()))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<CouponResponseDTO> getAllCoupons() {
        return couponRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new InvalidCouponException("Coupon not found with ID: " + couponId));
        couponRepository.delete(coupon);
    }

    public CouponResponseDTO convertToResponse(Coupon coupon) {
        return CouponResponseDTO.builder()
                .couponId(coupon.getCouponId())
                .couponCode(coupon.getCouponCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minimumBookingAmount(coupon.getMinimumBookingAmount())
                .maximumDiscount(coupon.getMaximumDiscount())
                .validFrom(coupon.getValidFrom())
                .validTo(coupon.getValidTo())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .active(coupon.getActive())
                .build();
    }
}
