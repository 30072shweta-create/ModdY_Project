package com.example.flight.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.flight.entity.AdjustmentType;
import com.example.flight.entity.RuleType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRuleResponseDTO {

    private Long ruleId;
    private String name;
    private RuleType ruleType;
    private AdjustmentType adjustmentType;
    private BigDecimal adjustmentValue;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private Boolean active;
    private Integer priority;
}
