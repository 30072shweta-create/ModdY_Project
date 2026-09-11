package com.example.flight.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PaymentStatusConverter implements AttributeConverter<PaymentStatus, String> {

    @Override
    public String convertToDatabaseColumn(PaymentStatus attribute) {
        if (attribute == null) {
            return PaymentStatus.SUCCESS.name();
        }
        return attribute.name();
    }

    @Override
    public PaymentStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return PaymentStatus.SUCCESS;
        }
        return PaymentStatus.fromString(dbData);
    }
}
