package com.buildflow.billing.dto;

import com.buildflow.billing.entity.GstInvoice;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GstInvoiceResponse(
        Long id,
        Long raBillId,
        String billNumber,
        Long projectId,
        String projectName,
        String invoiceNumber,
        LocalDate invoiceDate,
        String hsnSacCode,
        String description,
        BigDecimal taxableValue,
        BigDecimal gstRate,
        boolean interState,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal igstAmount,
        BigDecimal totalInvoiceValue,
        String placeOfSupply,
        String supplierName,
        String supplierGstin,
        String supplierAddress,
        String clientName,
        String clientGstin,
        String clientAddress,
        String createdByName
) {
    public static GstInvoiceResponse from(GstInvoice invoice) {
        return new GstInvoiceResponse(
                invoice.getId(),
                invoice.getRaBill().getId(),
                invoice.getRaBill().getBillNumber(),
                invoice.getRaBill().getProject().getId(),
                invoice.getRaBill().getProject().getName(),
                invoice.getInvoiceNumber(),
                invoice.getInvoiceDate(),
                invoice.getHsnSacCode(),
                invoice.getDescription(),
                invoice.getTaxableValue(),
                invoice.getGstRate(),
                invoice.isInterState(),
                invoice.getCgstAmount(),
                invoice.getSgstAmount(),
                invoice.getIgstAmount(),
                invoice.getTotalInvoiceValue(),
                invoice.getPlaceOfSupply(),
                invoice.getSupplierName(),
                invoice.getSupplierGstin(),
                invoice.getSupplierAddress(),
                invoice.getClientName(),
                invoice.getClientGstin(),
                invoice.getClientAddress(),
                invoice.getCreatedBy().getFullName()
        );
    }
}
