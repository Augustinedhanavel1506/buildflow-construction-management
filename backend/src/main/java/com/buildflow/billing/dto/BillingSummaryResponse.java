package com.buildflow.billing.dto;

import java.math.BigDecimal;

public record BillingSummaryResponse(
        BigDecimal totalBilled,
        BigDecimal totalReceived,
        BigDecimal totalOutstanding,
        BigDecimal totalRetentionHeld
) {
}
