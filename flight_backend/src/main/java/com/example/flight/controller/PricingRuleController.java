package com.example.flight.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.flight.dto.PricingRuleRequestDTO;
import com.example.flight.dto.PricingRuleResponseDTO;
import com.example.flight.service.PricingRuleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pricing-rules")
@RequiredArgsConstructor
@Tag(name = "Pricing Rules", description = "Endpoints for managing dynamic pricing rules (ADMIN only)")
public class PricingRuleController {

    private final PricingRuleService pricingRuleService;

    @Operation(summary = "Get all pricing rules")
    @GetMapping
    public List<PricingRuleResponseDTO> getAllRules() {
        return pricingRuleService.getAllRules();
    }

    @Operation(summary = "Create dynamic pricing rule (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public PricingRuleResponseDTO createRule(@Valid @RequestBody PricingRuleRequestDTO dto) {
        return pricingRuleService.createRule(dto);
    }

    @Operation(summary = "Update dynamic pricing rule (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{ruleId}")
    public PricingRuleResponseDTO updateRule(@PathVariable Long ruleId,
                                            @Valid @RequestBody PricingRuleRequestDTO dto) {
        return pricingRuleService.updateRule(ruleId, dto);
    }

    @Operation(summary = "Delete dynamic pricing rule (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{ruleId}")
    public void deleteRule(@PathVariable Long ruleId) {
        pricingRuleService.deleteRule(ruleId);
    }
}
