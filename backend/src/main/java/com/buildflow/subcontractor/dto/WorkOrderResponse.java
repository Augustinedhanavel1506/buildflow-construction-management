package com.buildflow.subcontractor.dto;

import com.buildflow.subcontractor.entity.SubcontractWorkOrder;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WorkOrderResponse(
        Long id,
        Long projectId,
        Long subcontractorId,
        String subcontractorName,
        String tradeType,
        String title,
        String scopeDescription,
        String contractType,
        BigDecimal contractValue,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalBilled,
        BigDecimal totalPaid
) {
    public static WorkOrderResponse from(SubcontractWorkOrder workOrder, BigDecimal totalBilled, BigDecimal totalPaid) {
        return new WorkOrderResponse(
                workOrder.getId(),
                workOrder.getProject().getId(),
                workOrder.getSubcontractor().getId(),
                workOrder.getSubcontractor().getName(),
                workOrder.getSubcontractor().getTradeType(),
                workOrder.getTitle(),
                workOrder.getScopeDescription(),
                workOrder.getContractType().name(),
                workOrder.getContractValue(),
                workOrder.getStatus().name(),
                workOrder.getStartDate(),
                workOrder.getEndDate(),
                totalBilled,
                totalPaid
        );
    }
}
