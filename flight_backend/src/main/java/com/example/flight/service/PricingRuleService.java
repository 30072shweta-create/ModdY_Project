package com.example.flight.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flight.dto.PricingRuleRequestDTO;
import com.example.flight.dto.PricingRuleResponseDTO;
import com.example.flight.entity.PricingRule;
import com.example.flight.repository.PricingRuleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PricingRuleService {

    private final PricingRuleRepository pricingRuleRepository;

    @Transactional(readOnly = true)
    public List<PricingRuleResponseDTO> getAllRules() {
        return pricingRuleRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public PricingRuleResponseDTO createRule(PricingRuleRequestDTO dto) {
        PricingRule rule = PricingRule.builder()
                .name(dto.getName())
                .ruleType(dto.getRuleType())
                .adjustmentType(dto.getAdjustmentType())
                .adjustmentValue(dto.getAdjustmentValue())
                .effectiveFrom(dto.getEffectiveFrom())
                .effectiveTo(dto.getEffectiveTo())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .priority(dto.getPriority() != null ? dto.getPriority() : 1)
                .build();

        PricingRule saved = pricingRuleRepository.save(rule);
        return convertToResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public PricingRuleResponseDTO updateRule(Long ruleId, PricingRuleRequestDTO dto) {
        PricingRule rule = pricingRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Pricing rule not found with ID: " + ruleId));

        rule.setName(dto.getName());
        rule.setRuleType(dto.getRuleType());
        rule.setAdjustmentType(dto.getAdjustmentType());
        rule.setAdjustmentValue(dto.getAdjustmentValue());
        rule.setEffectiveFrom(dto.getEffectiveFrom());
        rule.setEffectiveTo(dto.getEffectiveTo());
        if (dto.getActive() != null) rule.setActive(dto.getActive());
        if (dto.getPriority() != null) rule.setPriority(dto.getPriority());

        PricingRule saved = pricingRuleRepository.save(rule);
        return convertToResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteRule(Long ruleId) {
        PricingRule rule = pricingRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Pricing rule not found with ID: " + ruleId));
        pricingRuleRepository.delete(rule);
    }

    public PricingRuleResponseDTO convertToResponse(PricingRule rule) {
        return PricingRuleResponseDTO.builder()
                .ruleId(rule.getRuleId())
                .name(rule.getName())
                .ruleType(rule.getRuleType())
                .adjustmentType(rule.getAdjustmentType())
                .adjustmentValue(rule.getAdjustmentValue())
                .effectiveFrom(rule.getEffectiveFrom())
                .effectiveTo(rule.getEffectiveTo())
                .active(rule.getActive())
                .priority(rule.getPriority())
                .build();
    }
}
