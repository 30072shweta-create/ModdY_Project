package com.example.flight.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.flight.entity.AdjustmentType;
import com.example.flight.entity.RuleType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRuleRequestDTO {

    @NotBlank(message = "Rule name is required")
    private String name;

    @NotNull(message = "Rule type is required")
    private RuleType ruleType;

    @NotNull(message = "Adjustment type is required")
    private AdjustmentType adjustmentType;

    @NotNull(message = "Adjustment value is required")
    private BigDecimal adjustmentValue;

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd[ 'T'HH:mm[:ss]]")
    private LocalDateTime effectiveFrom;

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd[ 'T'HH:mm[:ss]]")
    private LocalDateTime effectiveTo;

    private Boolean active;
    private Integer priority;
}
