package com.buildflow.billing.dto;

import com.buildflow.billing.entity.RaBill;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RaBillResponse(
        Long id,
        Long projectId,
        String billNumber,
        LocalDate billDate,
        BigDecimal workDoneValue,
        BigDecimal retentionPercent,
        BigDecimal otherDeductions,
        BigDecimal certifiedAmount,
        BigDecimal retentionAmount,
        BigDecimal netPayableAmount,
        BigDecimal amountReceived,
        BigDecimal outstandingAmount,
        String status,
        String notes,
        String createdByName
) {
    public static RaBillResponse from(RaBill bill, BigDecimal amountReceived) {
        return new RaBillResponse(
                bill.getId(),
                bill.getProject().getId(),
                bill.getBillNumber(),
                bill.getBillDate(),
                bill.getWorkDoneValue(),
                bill.getRetentionPercent(),
                bill.getOtherDeductions(),
                bill.getCertifiedAmount(),
                bill.getRetentionAmount(),
                bill.getNetPayableAmount(),
                amountReceived,
                bill.getNetPayableAmount().subtract(amountReceived),
                bill.getStatus().name(),
                bill.getNotes(),
                bill.getCreatedBy().getFullName()
        );
    }
}
