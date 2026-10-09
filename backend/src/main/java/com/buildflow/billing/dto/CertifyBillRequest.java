package com.buildflow.billing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CertifyBillRequest(
        @NotNull(message = "Certified amount is required") @Positive(message = "Certified amount must be greater than zero") BigDecimal certifiedAmount
) {
}
