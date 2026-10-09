package com.buildflow.material.dto;

import com.buildflow.material.entity.MaterialUsage;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MaterialUsageResponse(
        Long id,
        Long materialId,
        String materialName,
        BigDecimal quantity,
        LocalDate usageDate,
        String notes,
        String createdByName
) {
    public static MaterialUsageResponse from(MaterialUsage usage) {
        return new MaterialUsageResponse(
                usage.getId(),
                usage.getMaterial().getId(),
                usage.getMaterial().getName(),
                usage.getQuantity(),
                usage.getUsageDate(),
                usage.getNotes(),
                usage.getCreatedBy().getFullName()
        );
    }
}
