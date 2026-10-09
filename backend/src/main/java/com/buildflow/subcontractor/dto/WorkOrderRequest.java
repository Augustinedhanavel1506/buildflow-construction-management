package com.buildflow.subcontractor.dto;

import com.buildflow.subcontractor.entity.ContractType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WorkOrderRequest(
        @NotNull(message = "Subcontractor is required") Long subcontractorId,
        @NotBlank(message = "Title is required") String title,
        @Size(max = 2000, message = "Scope description is too long") String scopeDescription,
        @NotNull(message = "Contract type is required") ContractType contractType,
        @NotNull(message = "Contract value is required") @Positive(message = "Contract value must be greater than zero") BigDecimal contractValue,
        LocalDate startDate,
        LocalDate endDate
) {
}
