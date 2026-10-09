package com.buildflow.material.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record MaterialRequest(
        @NotBlank(message = "Material name is required") String name,
        @NotBlank(message = "Unit is required") String unit,
        @NotNull(message = "Minimum stock is required") @PositiveOrZero(message = "Minimum stock cannot be negative") BigDecimal minimumStock,
        @PositiveOrZero(message = "Opening stock cannot be negative") BigDecimal openingStock
) {
}
