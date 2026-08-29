package com.example.flight.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.flight.entity.Coupon;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCouponCodeIgnoreCase(String couponCode);

    boolean existsByCouponCodeIgnoreCase(String couponCode);
}
