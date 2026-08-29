package com.example.flight.service;
//Determines add-on prices

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.example.flight.entity.AddOnType;

@Service
public class AddOnPricingService {

    public BigDecimal getUnitPrice(
            AddOnType addonType) {

        return switch (addonType) {

            case MEAL ->
                    new BigDecimal("300.00");

            case EXTRA_BAGGAGE ->
                    new BigDecimal("1000.00");

            case SPECIAL_ASSISTANCE ->
                    BigDecimal.ZERO;
        };
    }
}