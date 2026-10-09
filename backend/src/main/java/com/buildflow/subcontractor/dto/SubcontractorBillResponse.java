package com.buildflow.subcontractor.dto;

import com.buildflow.subcontractor.entity.SubcontractorBill;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubcontractorBillResponse(
        Long id,
        Long workOrderId,
        String billNumber,
        LocalDate billDate,
        BigDecimal workDoneValue,
        BigDecimal tdsPercent,
        BigDecimal retentionPercent,
        BigDecimal otherDeductions,
        BigDecimal tdsAmount,
        BigDecimal retentionAmount,
        BigDecimal netPayableAmount,
        BigDecimal amountPaid,
        BigDecimal outstandingAmount,
        String status,
        String notes,
        String createdByName
) {
    public static SubcontractorBillResponse from(SubcontractorBill bill, BigDecimal amountPaid) {
        return new SubcontractorBillResponse(
                bill.getId(),
                bill.getWorkOrder().getId(),
                bill.getBillNumber(),
                bill.getBillDate(),
                bill.getWorkDoneValue(),
                bill.getTdsPercent(),
                bill.getRetentionPercent(),
                bill.getOtherDeductions(),
                bill.getTdsAmount(),
                bill.getRetentionAmount(),
                bill.getNetPayableAmount(),
                amountPaid,
                bill.getNetPayableAmount().subtract(amountPaid),
                bill.getStatus().name(),
                bill.getNotes(),
                bill.getCreatedBy().getFullName()
        );
    }
}
