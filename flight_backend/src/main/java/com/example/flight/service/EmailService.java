package com.example.flight.service;

import lombok.RequiredArgsConstructor;

<<<<<<< HEAD
import org.springframework.beans.factory.annotation.Value;
=======
>>>>>>> c0433ef5f1e407a86a7aa70b8549f194d827e51f
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

<<<<<<< HEAD
    @Value("${spring.mail.username}")
    private String fromEmail;

=======
>>>>>>> c0433ef5f1e407a86a7aa70b8549f194d827e51f
    public void sendOtpEmail(
            String email,
            String otp,
            String purpose) {

        SimpleMailMessage message = new SimpleMailMessage();

<<<<<<< HEAD
        message.setFrom(fromEmail);
=======
>>>>>>> c0433ef5f1e407a86a7aa70b8549f194d827e51f
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
<<<<<<< HEAD
}
=======
}
>>>>>>> c0433ef5f1e407a86a7aa70b8549f194d827e51f
