package com.buildflow.estimation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record FloorRequirementRequest(
        @NotNull(message = "Floor level is required") @Min(value = 0, message = "Floor level cannot be negative") Integer floorLevel,
        @NotNull(message = "Floor area is required") @PositiveOrZero(message = "Floor area cannot be negative") BigDecimal floorAreaSqft,
        @PositiveOrZero(message = "Bedroom count cannot be negative") Integer bedroomCount,
        @PositiveOrZero(message = "Bathroom count cannot be negative") Integer bathroomCount,
        Boolean hasKitchen,
        Boolean hasHall,
        Boolean hasBalcony,
        Boolean hasPoojaRoom,
        @PositiveOrZero(message = "Door count cannot be negative") Integer doorCount,
        @PositiveOrZero(message = "Window count cannot be negative") Integer windowCount
) {
}
