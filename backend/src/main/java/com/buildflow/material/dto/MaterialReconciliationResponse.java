package com.buildflow.material.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MaterialReconciliationResponse(
        List<MaterialReconciliationRow> materials,
        BigDecimal totalEstimatedVarianceValue
) {
    public record MaterialReconciliationRow(
            Long materialId,
            String materialName,
            String unit,
            BigDecimal currentStock,
            BigDecimal totalPurchased,
            BigDecimal totalUsed,
            BigDecimal averageRate,
            LocalDate lastCountDate,
            BigDecimal lastCountVariance,
            BigDecimal cumulativeVarianceQty,
            BigDecimal estimatedVarianceValue
    ) {
    }
}
