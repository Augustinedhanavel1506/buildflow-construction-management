package com.buildflow.report.dto;

import com.buildflow.boq.dto.BoqItemResponse;

import java.math.BigDecimal;
import java.util.List;

public record BoqVarianceReportResponse(
        List<BoqItemResponse> items,
        BigDecimal totalEstimated,
        BigDecimal totalActual,
        BigDecimal totalVariance
) {
}
