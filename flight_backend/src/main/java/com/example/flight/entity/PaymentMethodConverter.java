package com.example.flight.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PaymentMethodConverter implements AttributeConverter<PaymentMethod, String> {

    @Override
    public String convertToDatabaseColumn(PaymentMethod attribute) {
        if (attribute == null) {
            return PaymentMethod.CARD.name();
        }
        return attribute.name();
    }

    @Override
    public PaymentMethod convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return PaymentMethod.CARD;
        }
        return PaymentMethod.fromString(dbData);
    }
}
