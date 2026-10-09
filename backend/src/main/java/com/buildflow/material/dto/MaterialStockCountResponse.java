package com.buildflow.material.dto;

import com.buildflow.material.entity.MaterialStockCount;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MaterialStockCountResponse(
        Long id,
        Long materialId,
        String materialName,
        LocalDate countDate,
        BigDecimal systemStock,
        BigDecimal countedStock,
        BigDecimal variance,
        String notes,
        String countedByName
) {
    public static MaterialStockCountResponse from(MaterialStockCount count) {
        return new MaterialStockCountResponse(
                count.getId(),
                count.getMaterial().getId(),
                count.getMaterial().getName(),
                count.getCountDate(),
                count.getSystemStock(),
                count.getCountedStock(),
                count.getVariance(),
                count.getNotes(),
                count.getCountedBy().getFullName()
        );
    }
}
