package com.example.flight.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.flight.entity.Payment;
import com.example.flight.entity.PaymentStatus;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBookingBookingId(Long bookingId);

    List<Payment> findByBookingBookingIdOrderByCreatedAtDesc(Long bookingId);

    List<Payment> findByBookingUserEmailOrderByCreatedAtDesc(String email);

    Optional<Payment> findByTransactionRef(String transactionRef);

    // ADD THIS FOR RAZORPAY
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    // Optional: useful for Razorpay refund/payment lookup
    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    boolean existsByTransactionRef(String transactionRef);

    boolean existsByBookingBookingIdAndStatus(
            Long bookingId,
            PaymentStatus status
    );

    Optional<Payment> findFirstByBookingBookingIdAndStatusOrderByCreatedAtDesc(
            Long bookingId,
            PaymentStatus status
    );
}