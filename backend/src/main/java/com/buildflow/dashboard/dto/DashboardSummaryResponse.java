package com.buildflow.dashboard.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DashboardSummaryResponse(
        ProjectCounts projectCounts,
        FinancialSummary financialSummary,
        BillingSummary billingSummary,
        BigDecimal estimatedMaterialVarianceValue,
        int overallProgress,
        List<CategoryAmount> costBreakdown,
        List<ActivityItem> recentActivity
) {
    public record ProjectCounts(
            long totalProjects,
            long activeProjects,
            long delayedProjects,
            long completedProjects
    ) {
    }

    public record FinancialSummary(
            BigDecimal totalContractValue,
            BigDecimal totalEstimatedCost,
            BigDecimal totalActualCost,
            int budgetUtilizationPercent
    ) {
    }

    public record CategoryAmount(String category, BigDecimal amount) {
    }

    public record BillingSummary(
            BigDecimal totalBilled,
            BigDecimal totalReceived,
            BigDecimal totalOutstanding,
            BigDecimal totalRetentionHeld
    ) {
    }

    public record ActivityItem(String type, String description, String projectName, Instant occurredAt) {
    }
}
