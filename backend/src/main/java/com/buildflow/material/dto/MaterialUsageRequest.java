package com.buildflow.material.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MaterialUsageRequest(
        @NotNull(message = "Quantity is required") @Positive(message = "Quantity must be greater than zero") BigDecimal quantity,
        @NotNull(message = "Usage date is required") LocalDate usageDate,
        @Size(max = 500, message = "Notes are too long") String notes
) {
}
