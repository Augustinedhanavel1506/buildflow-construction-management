package com.buildflow.subcontractor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SubcontractorRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Trade type is required") String tradeType,
        String contactPerson,
        String phone,
        @Email(message = "Enter a valid email address") String email,
        String gstNumber,
        String panNumber,
        Boolean active
) {
}
