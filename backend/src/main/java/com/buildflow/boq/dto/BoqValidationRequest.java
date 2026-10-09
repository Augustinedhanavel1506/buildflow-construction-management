package com.buildflow.boq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// quantity is the engineer's corrected quantity; omit it to approve the estimated quantity as is.
// It is ignored by the project-wide approval, which only approves lines unchanged.
public record BoqValidationRequest(
        @NotBlank(message = "Engineer name is required") @Size(max = 120, message = "Engineer name is too long") String engineerName,
        @Positive(message = "Quantity must be greater than zero") BigDecimal quantity,
        @Size(max = 500, message = "Note is too long") String note
) {
}
