package com.example.flight.dto;

import com.example.flight.entity.AddOnType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingAddOnRequestDTO {

    @NotNull(message = "Add-on type is required")
    private AddOnType addonType;


    private String description;


    @Min(
        value = 1,
        message = "Quantity must be at least 1"
    )
    private Integer quantity = 1;
}