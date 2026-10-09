package com.buildflow.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProfitabilityReportResponse(
        List<ProjectProfitability> projects,
        BigDecimal totalContractValue,
        BigDecimal totalEstimatedFinalCost,
        BigDecimal totalEstimatedMargin
) {
    public record ProjectProfitability(
            Long projectId,
            String projectName,
            String status,
            BigDecimal contractValue,
            BigDecimal revisedContractValue,
            BigDecimal estimatedFinalCost,
            BigDecimal estimatedMargin,
            BigDecimal marginPercent
    ) {
    }
}
