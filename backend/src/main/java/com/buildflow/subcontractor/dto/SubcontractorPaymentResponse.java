package com.buildflow.subcontractor.dto;

import com.buildflow.subcontractor.entity.SubcontractorPayment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubcontractorPaymentResponse(
        Long id,
        Long subcontractorBillId,
        BigDecimal amount,
        LocalDate paymentDate,
        String mode,
        String referenceNumber,
        String recordedByName
) {
    public static SubcontractorPaymentResponse from(SubcontractorPayment payment) {
        return new SubcontractorPaymentResponse(
                payment.getId(),
                payment.getSubcontractorBill().getId(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getMode().name(),
                payment.getReferenceNumber(),
                payment.getRecordedBy().getFullName()
        );
    }
}
