package com.buildflow.labour.dto;

import java.math.BigDecimal;
import java.util.List;

public record LabourCostResponse(
        BigDecimal totalCost,
        List<RoleBreakdown> byRole
) {
    public record RoleBreakdown(String role, long presentDays, BigDecimal cost) {
    }
}
