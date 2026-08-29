package com.example.flight.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.flight.entity.PricingRule;
import com.example.flight.entity.RuleType;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {

    List<PricingRule> findByRuleTypeAndActiveTrueOrderByPriorityDesc(RuleType ruleType);

    @Query("SELECT r FROM PricingRule r WHERE r.ruleType = :ruleType AND r.active = true AND (r.effectiveFrom IS NULL OR r.effectiveFrom <= :timestamp) AND (r.effectiveTo IS NULL OR r.effectiveTo >= :timestamp) ORDER BY r.priority DESC")
    List<PricingRule> findActiveRulesByTypeAndTimestamp(
            @Param("ruleType") RuleType ruleType,
            @Param("timestamp") LocalDateTime timestamp
    );

    @Query("SELECT r FROM PricingRule r WHERE r.active = true AND (r.effectiveFrom IS NULL OR r.effectiveFrom <= :timestamp) AND (r.effectiveTo IS NULL OR r.effectiveTo >= :timestamp) ORDER BY r.priority DESC")
    List<PricingRule> findAllActiveRulesAtTimestamp(
            @Param("timestamp") LocalDateTime timestamp
    );
}
