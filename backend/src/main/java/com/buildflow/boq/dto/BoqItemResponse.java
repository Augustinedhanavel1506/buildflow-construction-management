package com.buildflow.boq.dto;

import com.buildflow.boq.entity.BoqItem;

import java.math.BigDecimal;

public record BoqItemResponse(
        Long id,
        Long projectId,
        String itemName,
        String category,
        String unit,
        BigDecimal quantity,
        BigDecimal quantityLow,
        BigDecimal quantityHigh,
        BigDecimal rate,
        BigDecimal estimatedAmount,
        BigDecimal actualAmount,
        BigDecimal variance,
        String estimateSource,
        String sourceRuleCode,
        String component,
        String validatedBy,
        java.time.Instant validatedAt,
        String validationNote
) {
    public static BoqItemResponse from(BoqItem item) {
        BigDecimal variance = item.getActualAmount().subtract(item.getEstimatedAmount());
        return new BoqItemResponse(
                item.getId(),
                item.getProject().getId(),
                item.getItemName(),
                item.getCategory().name(),
                item.getUnit(),
                item.getQuantity(),
                item.getQuantityLow(),
                item.getQuantityHigh(),
                item.getRate(),
                item.getEstimatedAmount(),
                item.getActualAmount(),
                variance,
                item.getEstimateSource().name(),
                item.getSourceRuleCode(),
                item.getComponent(),
                item.getValidatedBy(),
                item.getValidatedAt(),
                item.getValidationNote()
        );
    }
}
