package com.example.flight.tools;

import java.util.List;

import com.example.flight.dto.PricingRuleResponseDTO;
import com.example.flight.service.PricingRuleService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class CouponTools {

    private final PricingRuleService pricingRuleService;

    public CouponTools(PricingRuleService pricingRuleService) {
        this.pricingRuleService = pricingRuleService;
    }

    @Tool(description = """
            Get live pricing rule information from the database.

            Use this tool when the user asks:
            - what pricing rules are active
            - what dynamic pricing adjustments exist
            - what discounts or markups are configured
            - how flight prices may be adjusted

            This returns pricing rules, not individual coupon-code validation.
            """)
    public List<PricingRuleResponseDTO> getPricingRules() {

        System.out.println("PRICING RULE TOOL CALLED");

        return pricingRuleService.getAllRules();
    }
}
