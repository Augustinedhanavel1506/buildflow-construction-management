package com.buildflow.material.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MaterialRequisitionRequest(
        @NotNull(message = "Material is required") Long materialId,
        @NotNull(message = "Quantity is required") @Positive(message = "Quantity must be greater than zero") BigDecimal quantity,
        LocalDate requiredDate,
        @Size(max = 500, message = "Reason is too long") String reason
) {
}
