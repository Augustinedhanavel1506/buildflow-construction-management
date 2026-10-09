package com.buildflow.estimation.dto;

import com.buildflow.estimation.entity.BoqGenerationRule;

import java.math.BigDecimal;

public record BoqGenerationRuleResponse(
        Long id,
        String ruleCode,
        String baseRuleCode,
        String component,
        String constructionGrade,
        String itemName,
        String boqCategory,
        String unit,
        String basis,
        BigDecimal coefficient,
        BigDecimal wastagePercent,
        BigDecimal minCoefficient,
        BigDecimal maxCoefficient,
        String sourceType,
        String sourceReference,
        String confidenceLevel,
        boolean verified,
        Integer sampleSize,
        int version,
        boolean active,
        String notes,
        String scope
) {
    public static BoqGenerationRuleResponse from(BoqGenerationRule rule) {
        return new BoqGenerationRuleResponse(
                rule.getId(),
                rule.getRuleCode(),
                rule.getBaseRuleCode() != null ? rule.getBaseRuleCode() : rule.getRuleCode(),
                rule.getComponent().name(),
                rule.getConstructionGrade().name(),
                rule.getItemName(),
                rule.getBoqCategory().name(),
                rule.getUnit(),
                rule.getBasis().name(),
                rule.getCoefficient(),
                rule.getWastagePercent(),
                rule.getMinCoefficient(),
                rule.getMaxCoefficient(),
                rule.getSourceType().name(),
                rule.getSourceReference(),
                rule.getConfidenceLevel().name(),
                rule.isVerified(),
                rule.getSampleSize(),
                rule.getVersion(),
                rule.isActive(),
                rule.getNotes(),
                rule.getBusiness() == null ? "PLATFORM" : "BUSINESS"
        );
    }
}
