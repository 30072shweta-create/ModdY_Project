package com.example.flight.dto;

import lombok.Data;

@Data
public class AirportResponseDTO {

    private String airportCode;

    private String name;

    private String city;

    private String country;
}