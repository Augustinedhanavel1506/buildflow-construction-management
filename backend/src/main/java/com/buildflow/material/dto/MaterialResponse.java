package com.buildflow.material.dto;

import com.buildflow.material.entity.Material;
import com.buildflow.material.entity.MaterialStockStatus;

import java.math.BigDecimal;

public record MaterialResponse(
        Long id,
        Long projectId,
        String name,
        String unit,
        BigDecimal currentStock,
        BigDecimal minimumStock,
        String status
) {
    public static MaterialResponse from(Material material) {
        return new MaterialResponse(
                material.getId(),
                material.getProject().getId(),
                material.getName(),
                material.getUnit(),
                material.getCurrentStock(),
                material.getMinimumStock(),
                resolveStatus(material).name()
        );
    }

    private static MaterialStockStatus resolveStatus(Material material) {
        if (material.getCurrentStock().signum() <= 0) {
            return MaterialStockStatus.CRITICAL;
        }
        if (material.getCurrentStock().compareTo(material.getMinimumStock()) < 0) {
            return MaterialStockStatus.LOW;
        }
        return MaterialStockStatus.NORMAL;
    }
}
