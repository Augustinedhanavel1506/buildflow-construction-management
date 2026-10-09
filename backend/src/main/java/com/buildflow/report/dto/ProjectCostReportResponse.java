package com.buildflow.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProjectCostReportResponse(
        Long projectId,
        String projectName,
        BigDecimal contractValue,
        BigDecimal revisedContractValue,
        BigDecimal estimatedCost,
        BigDecimal actualCost,
        BigDecimal remainingEstimatedCost,
        BigDecimal estimatedFinalCost,
        BigDecimal estimatedMargin,
        int overallProgress,
        List<CategoryAmount> expenseBreakdown,
        BoqVarianceTotals boqVariance
) {
    public record CategoryAmount(String category, BigDecimal amount) {
    }

    public record BoqVarianceTotals(BigDecimal totalEstimated, BigDecimal totalActual, BigDecimal totalVariance) {
    }
}
