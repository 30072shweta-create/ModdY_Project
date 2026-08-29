package com.example.flight.service;

import lombok.RequiredArgsConstructor;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(
            String email,
            String otp,
            String purpose) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);

        if (purpose.equals("EMAIL_VERIFICATION")) {

            message.setSubject("Flight Booking - Email Verification");

            message.setText(
                    "Your email verification OTP is: " + otp +
                    "\n\nThis OTP is valid for 10 minutes."
            );

        } else {

            message.setSubject("Flight Booking - Password Reset");

            message.setText(
                    "Your password reset OTP is: " + otp +
                    "\n\nThis OTP is valid for 10 minutes."
            );
        }

        mailSender.send(message);
    }
}