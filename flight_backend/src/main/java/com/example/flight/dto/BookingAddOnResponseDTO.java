package com.example.flight.dto;

import java.math.BigDecimal;

import com.example.flight.entity.AddOnType;

import lombok.Data;

@Data
public class BookingAddOnResponseDTO {

    private Long addonId;

    private Long bookingId;

    private AddOnType addonType;

    private String description;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;
}