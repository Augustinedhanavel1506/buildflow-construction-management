package com.buildflow.subcontractor.dto;

import com.buildflow.billing.entity.PaymentMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubcontractorPaymentRequest(
        @NotNull(message = "Amount is required") @Positive(message = "Amount must be greater than zero") BigDecimal amount,
        @NotNull(message = "Payment date is required") LocalDate paymentDate,
        @NotNull(message = "Payment mode is required") PaymentMode mode,
        @Size(max = 255, message = "Reference number is too long") String referenceNumber
) {
}
