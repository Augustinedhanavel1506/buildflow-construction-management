package com.buildflow.labour.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record WorkerRequest(
        @NotBlank(message = "Worker name is required") String name,
        @NotBlank(message = "Role is required") String role,
        @NotNull(message = "Daily rate is required") @Positive(message = "Daily rate must be greater than zero") BigDecimal dailyRate,
        Boolean active
) {
}
