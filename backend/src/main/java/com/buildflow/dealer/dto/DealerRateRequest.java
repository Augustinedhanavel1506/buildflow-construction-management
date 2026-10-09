package com.buildflow.dealer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record DealerRateRequest(
        @NotBlank(message = "Item name is required") String itemName,
        @NotBlank(message = "Unit is required") String unit,
        @NotNull(message = "Rate is required") @Positive(message = "Rate must be greater than zero") BigDecimal rate
) {
}
