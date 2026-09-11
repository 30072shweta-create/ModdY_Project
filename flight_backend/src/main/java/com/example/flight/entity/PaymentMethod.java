package com.example.flight.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentMethod {
    RAZORPAY,
    UPI,
    CARD,
    CREDIT_CARD,
    DEBIT_CARD,
    NET_BANKING,
    WALLET,

    credit_card,
    debit_card,
    net_banking,
    upi,
    razorpay,
    wallet;

    @JsonValue
    public String toValue() {
        return this.name();
    }

    @JsonCreator
    public static PaymentMethod fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return CARD;
        }
        String cleaned = value.trim();
        for (PaymentMethod pm : values()) {
            if (pm.name().equalsIgnoreCase(cleaned) || pm.name().equalsIgnoreCase(cleaned.replace("-", "_").replace(" ", "_"))) {
                return pm;
            }
        }
        String normalized = cleaned.toUpperCase().replace("-", "_").replace(" ", "_");
        if (normalized.contains("CREDIT_CARD")) {
            return CREDIT_CARD;
        } else if (normalized.contains("DEBIT_CARD")) {
            return DEBIT_CARD;
        } else if (normalized.contains("CARD")) {
            return CARD;
        } else if (normalized.contains("UPI")) {
            return UPI;
        } else if (normalized.contains("NET") || normalized.contains("BANK")) {
            return NET_BANKING;
        } else if (normalized.contains("WALLET")) {
            return WALLET;
        } else if (normalized.contains("RAZOR")) {
            return RAZORPAY;
        }
        return CARD;
    }
}