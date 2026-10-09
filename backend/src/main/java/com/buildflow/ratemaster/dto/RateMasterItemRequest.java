package com.buildflow.ratemaster.dto;

import com.buildflow.boq.entity.BoqCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RateMasterItemRequest(
        @NotBlank(message = "Item name is required") String itemName,
        @NotNull(message = "Category is required") BoqCategory category,
        @NotBlank(message = "Unit is required") String unit,
        @NotNull(message = "Standard rate is required") @Positive(message = "Standard rate must be greater than zero") BigDecimal standardRate,
        @Positive(message = "Minimum rate must be greater than zero") BigDecimal minRate,
        @Positive(message = "Maximum rate must be greater than zero") BigDecimal maxRate,
        @PositiveOrZero(message = "Consumption per sqft cannot be negative") BigDecimal consumptionPerSqft,
        @Size(max = 500, message = "Notes are too long") String notes,
        Boolean active,
        @Size(max = 100, message = "District is too long") String district
) {
}
