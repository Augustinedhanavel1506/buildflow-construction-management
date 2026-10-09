package com.buildflow.estimation.dto;

import com.buildflow.estimation.entity.FloorRequirement;

import java.math.BigDecimal;

public record FloorRequirementResponse(
        Long id,
        int floorLevel,
        BigDecimal floorAreaSqft,
        int bedroomCount,
        int bathroomCount,
        boolean hasKitchen,
        boolean hasHall,
        boolean hasBalcony,
        boolean hasPoojaRoom,
        int doorCount,
        int windowCount
) {
    public static FloorRequirementResponse from(FloorRequirement floor) {
        return new FloorRequirementResponse(
                floor.getId(),
                floor.getFloorLevel(),
                floor.getFloorAreaSqft(),
                floor.getBedroomCount(),
                floor.getBathroomCount(),
                floor.isHasKitchen(),
                floor.isHasHall(),
                floor.isHasBalcony(),
                floor.isHasPoojaRoom(),
                floor.getDoorCount(),
                floor.getWindowCount()
        );
    }
}
