package com.buildflow.estimation.dto;

import com.buildflow.estimation.entity.ConstructionGrade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public final class WhatIfModels {

    private WhatIfModels() {
    }

    // Any field left null keeps the plan's current value for that floor.
    public record FloorChange(
            @NotNull(message = "Floor level is required") Integer floorLevel,
            @Positive(message = "Floor area must be greater than zero") BigDecimal floorAreaSqft,
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

    public record NewFloor(
            @NotNull(message = "Floor area is required") @Positive(message = "Floor area must be greater than zero") BigDecimal floorAreaSqft,
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

    public record WhatIfRequest(
            ConstructionGrade constructionGrade,
            List<@Valid FloorChange> floors,
            List<Integer> removeFloorLevels,
            List<@Valid NewFloor> addFloors
    ) {
    }

    public record Summary(BigDecimal total, BigDecimal builtUpAreaSqft) {
    }

    public record ComponentDiff(String component, BigDecimal baseline, BigDecimal scenario, BigDecimal difference) {
    }

    public record ItemDiff(String itemName, String unit, BigDecimal baselineQuantity, BigDecimal scenarioQuantity,
                           BigDecimal baselineAmount, BigDecimal scenarioAmount, BigDecimal difference) {
    }

    public record WhatIfResponse(
            Summary baseline,
            Summary scenario,
            BigDecimal difference,
            // Null when the baseline total is zero.
            BigDecimal differencePercent,
            List<ComponentDiff> components,
            List<ItemDiff> items,
            List<String> unpricedItems
    ) {
    }
}
