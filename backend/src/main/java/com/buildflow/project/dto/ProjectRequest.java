package com.buildflow.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectRequest(
        @NotBlank(message = "Project name is required") String name,
        String clientName,
        String clientGstin,
        String clientAddress,
        String location,
        @NotNull(message = "Contract value is required") @Positive(message = "Contract value must be greater than zero") BigDecimal contractValue,
        @PositiveOrZero(message = "Estimated cost cannot be negative") BigDecimal estimatedCost,
        LocalDate startDate,
        LocalDate expectedEndDate
) {
}
