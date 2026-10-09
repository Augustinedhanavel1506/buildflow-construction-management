package com.buildflow.billing.dto;

import com.buildflow.billing.entity.BillPayment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillPaymentResponse(
        Long id,
        Long raBillId,
        BigDecimal amount,
        LocalDate paymentDate,
        String mode,
        String referenceNumber,
        String recordedByName
) {
    public static BillPaymentResponse from(BillPayment payment) {
        return new BillPaymentResponse(
                payment.getId(),
                payment.getRaBill().getId(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getMode().name(),
                payment.getReferenceNumber(),
                payment.getRecordedBy().getFullName()
        );
    }
}
