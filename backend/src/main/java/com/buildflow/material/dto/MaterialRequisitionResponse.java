package com.buildflow.material.dto;

import com.buildflow.material.entity.MaterialRequisition;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MaterialRequisitionResponse(
        Long id,
        Long projectId,
        Long materialId,
        String materialName,
        BigDecimal quantity,
        LocalDate requiredDate,
        String reason,
        String status,
        String requestedByName,
        String decidedByName
) {
    public static MaterialRequisitionResponse from(MaterialRequisition requisition) {
        return new MaterialRequisitionResponse(
                requisition.getId(),
                requisition.getProject().getId(),
                requisition.getMaterial().getId(),
                requisition.getMaterial().getName(),
                requisition.getQuantity(),
                requisition.getRequiredDate(),
                requisition.getReason(),
                requisition.getStatus().name(),
                requisition.getRequestedBy().getFullName(),
                requisition.getDecidedBy() != null ? requisition.getDecidedBy().getFullName() : null
        );
    }
}
