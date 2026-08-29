package com.example.flight.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.flight.entity.EmailOtp;
import com.example.flight.entity.OtpType;

public interface EmailOtpRepository
        extends JpaRepository<EmailOtp, Long> {

    Optional<EmailOtp> findTopByEmailAndTypeAndUsedFalseOrderByIdDesc(
            String email,
            OtpType type
    );
}