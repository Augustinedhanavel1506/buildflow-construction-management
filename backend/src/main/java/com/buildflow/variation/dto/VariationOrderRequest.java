package com.buildflow.variation.dto;

import com.buildflow.variation.entity.VariationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record VariationOrderRequest(
        @NotBlank(message = "Title is required") String title,
        @Size(max = 1000, message = "Description is too long") String description,
        @NotNull(message = "Type is required") VariationType type,
        @NotNull(message = "Amount is required") @Positive(message = "Amount must be greater than zero") BigDecimal amount
) {
}
