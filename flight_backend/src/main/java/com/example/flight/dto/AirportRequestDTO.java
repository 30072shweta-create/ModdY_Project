package com.example.flight.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AirportRequestDTO {

    @NotBlank
    private String airportCode;

    @NotBlank
    private String name;

    @NotBlank
    private String city;

    @NotBlank
    private String country;
}