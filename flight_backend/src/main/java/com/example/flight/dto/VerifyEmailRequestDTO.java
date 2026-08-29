package com.example.flight.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyEmailRequestDTO {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String otp;
}