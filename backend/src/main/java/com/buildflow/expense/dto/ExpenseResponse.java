package com.buildflow.expense.dto;

import com.buildflow.expense.entity.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(
        Long id,
        Long projectId,
        String category,
        BigDecimal amount,
        LocalDate date,
        String supplierName,
        String description,
        String createdByName
) {
    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getProject().getId(),
                expense.getCategory().name(),
                expense.getAmount(),
                expense.getDate(),
                expense.getSupplierName(),
                expense.getDescription(),
                expense.getCreatedBy().getFullName()
        );
    }
}
