package com.buildflow.material.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MaterialPurchaseRequest(
        @NotNull(message = "Quantity is required") @Positive(message = "Quantity must be greater than zero") BigDecimal quantity,
        @NotNull(message = "Rate is required") @Positive(message = "Rate must be greater than zero") BigDecimal rate,
        @Size(max = 255, message = "Supplier name is too long") String supplierName,
        @NotNull(message = "Purchase date is required") LocalDate purchaseDate
) {
}
