package com.buildflow.material.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MaterialStockCountRequest(
        @NotNull(message = "Count date is required") LocalDate countDate,
        @NotNull(message = "Counted stock is required") @PositiveOrZero(message = "Counted stock cannot be negative") BigDecimal countedStock,
        @Size(max = 500, message = "Notes are too long") String notes
) {
}
