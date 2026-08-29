package com.example.flight.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.flight.entity.Refund;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByBookingBookingId(Long bookingId);

    Optional<Refund> findByRefundReference(String refundReference);

    List<Refund> findByPaymentPaymentId(Long paymentId);

    boolean existsByBookingBookingId(Long bookingId);
}
