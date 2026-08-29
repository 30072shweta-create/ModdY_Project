package com.example.flight.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileRequestDTO {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;
}