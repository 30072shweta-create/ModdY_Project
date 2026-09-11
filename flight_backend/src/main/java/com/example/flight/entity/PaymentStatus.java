package com.example.flight.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PaymentStatus {
    CREATED,
    PENDING,
    SUCCESS,
    FAILED,
    REFUND_PENDING,
    REFUNDED, 
    PAID,

    created,
    pending,
    success,
    failed,
    refund_pending,
    refunded,
    paid;

    @JsonCreator
    public static PaymentStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return SUCCESS;
        }
        String cleaned = value.trim();
        for (PaymentStatus ps : values()) {
            if (ps.name().equalsIgnoreCase(cleaned) || ps.name().equalsIgnoreCase(cleaned.replace("-", "_").replace(" ", "_"))) {
                return ps;
            }
        }
        String normalized = cleaned.toUpperCase().replace("-", "_").replace(" ", "_");
        if (normalized.contains("FAIL")) {
            return FAILED;
        } else if (normalized.contains("REFUND")) {
            return REFUNDED;
        } else if (normalized.contains("PEND")) {
            return PENDING;
        } else if (normalized.contains("PAID") || normalized.contains("SUCC")) {
            return SUCCESS;
        }
        return SUCCESS;
    }
}