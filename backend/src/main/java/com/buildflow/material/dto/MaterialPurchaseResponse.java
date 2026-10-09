package com.buildflow.material.dto;

import com.buildflow.material.entity.MaterialPurchase;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MaterialPurchaseResponse(
        Long id,
        Long materialId,
        String materialName,
        BigDecimal quantity,
        BigDecimal rate,
        BigDecimal amount,
        String supplierName,
        LocalDate purchaseDate,
        String createdByName
) {
    public static MaterialPurchaseResponse from(MaterialPurchase purchase) {
        return new MaterialPurchaseResponse(
                purchase.getId(),
                purchase.getMaterial().getId(),
                purchase.getMaterial().getName(),
                purchase.getQuantity(),
                purchase.getRate(),
                purchase.getQuantity().multiply(purchase.getRate()),
                purchase.getSupplierName(),
                purchase.getPurchaseDate(),
                purchase.getCreatedBy().getFullName()
        );
    }
}
