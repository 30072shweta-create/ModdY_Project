package com.example.flight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AirlineRequestDTO {

    @NotBlank(message = "Airline code is required")
    @Size(max = 3, message = "Airline code cannot exceed 3 characters")
    private String airlineCode;

    @NotBlank(message = "Airline name is required")
    @Size(max = 200, message = "Airline name cannot exceed 200 characters")
    private String airlineName;
}