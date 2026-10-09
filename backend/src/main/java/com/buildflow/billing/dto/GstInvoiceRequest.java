package com.buildflow.billing.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GstInvoiceRequest(
        @NotBlank(message = "Invoice number is required") String invoiceNumber,
        @NotNull(message = "Invoice date is required") LocalDate invoiceDate,
        String hsnSacCode,
        @DecimalMin(value = "0", message = "GST rate cannot be negative")
        @DecimalMax(value = "100", message = "GST rate cannot exceed 100")
        BigDecimal gstRate,
        @NotNull(message = "Please specify whether this is an inter-state supply") Boolean interState,
        String placeOfSupply,
        @Size(max = 1000, message = "Description is too long") String description
) {
}
