package com.buildflow.billing.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RaBillRequest(
        @NotBlank(message = "Bill number is required") String billNumber,
        @NotNull(message = "Bill date is required") LocalDate billDate,
        @NotNull(message = "Work done value is required") @Positive(message = "Work done value must be greater than zero") BigDecimal workDoneValue,
        @NotNull(message = "Retention percent is required")
        @DecimalMin(value = "0", message = "Retention percent cannot be negative")
        @DecimalMax(value = "100", message = "Retention percent cannot exceed 100")
        BigDecimal retentionPercent,
        @PositiveOrZero(message = "Other deductions cannot be negative") BigDecimal otherDeductions,
        @Size(max = 1000, message = "Notes are too long") String notes
) {
}
