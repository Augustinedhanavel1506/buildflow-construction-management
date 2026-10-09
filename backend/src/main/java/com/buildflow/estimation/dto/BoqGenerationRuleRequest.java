package com.buildflow.estimation.dto;

import com.buildflow.estimation.entity.ConfidenceLevel;
import com.buildflow.estimation.entity.RuleSourceType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record BoqGenerationRuleRequest(
        @NotNull(message = "Coefficient is required") @PositiveOrZero(message = "Coefficient cannot be negative") BigDecimal coefficient,
        @NotNull(message = "Wastage percent is required") @PositiveOrZero(message = "Wastage cannot be negative")
        @DecimalMax(value = "100", message = "Wastage cannot exceed 100%") BigDecimal wastagePercent,
        @PositiveOrZero(message = "Minimum coefficient cannot be negative") BigDecimal minCoefficient,
        @PositiveOrZero(message = "Maximum coefficient cannot be negative") BigDecimal maxCoefficient,
        @NotNull(message = "Source type is required") RuleSourceType sourceType,
        @NotBlank(message = "Source reference is required") @Size(max = 500, message = "Source reference is too long") String sourceReference,
        @NotNull(message = "Confidence level is required") ConfidenceLevel confidenceLevel,
        Boolean verified,
        @PositiveOrZero(message = "Sample size cannot be negative") Integer sampleSize,
        @Size(max = 500, message = "Notes are too long") String notes,
        Boolean active
) {
}
