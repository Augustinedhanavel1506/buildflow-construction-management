package com.buildflow.expense.dto;

import com.buildflow.expense.entity.ExpenseCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotNull(message = "Category is required") ExpenseCategory category,
        @NotNull(message = "Amount is required") @Positive(message = "Amount must be greater than zero") BigDecimal amount,
        @NotNull(message = "Date is required") LocalDate date,
        @Size(max = 255, message = "Supplier name is too long") String supplierName,
        @Size(max = 1000, message = "Description is too long") String description
) {
}
