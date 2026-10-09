package com.buildflow.boq.dto;

import com.buildflow.boq.entity.BoqCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record BoqItemRequest(
        @NotBlank(message = "Item name is required") String itemName,
        @NotNull(message = "Category is required") BoqCategory category,
        @NotBlank(message = "Unit is required") String unit,
        @NotNull(message = "Quantity is required") @Positive(message = "Quantity must be greater than zero") BigDecimal quantity,
        @NotNull(message = "Rate is required") @Positive(message = "Rate must be greater than zero") BigDecimal rate,
        @PositiveOrZero(message = "Actual amount cannot be negative") BigDecimal actualAmount
) {
}
