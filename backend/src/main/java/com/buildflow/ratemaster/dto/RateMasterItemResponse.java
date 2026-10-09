package com.buildflow.ratemaster.dto;

import com.buildflow.ratemaster.entity.RateMasterItem;

import java.math.BigDecimal;

public record RateMasterItemResponse(
        Long id,
        String itemName,
        String category,
        String unit,
        BigDecimal standardRate,
        BigDecimal minRate,
        BigDecimal maxRate,
        BigDecimal consumptionPerSqft,
        String notes,
        boolean active,
        String district
) {
    public static RateMasterItemResponse from(RateMasterItem item) {
        return new RateMasterItemResponse(
                item.getId(),
                item.getItemName(),
                item.getCategory().name(),
                item.getUnit(),
                item.getStandardRate(),
                item.getMinRate(),
                item.getMaxRate(),
                item.getConsumptionPerSqft(),
                item.getNotes(),
                item.isActive(),
                item.getDistrict()
        );
    }
}
