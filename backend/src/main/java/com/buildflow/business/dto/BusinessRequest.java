package com.buildflow.business.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record BusinessRequest(
        @NotBlank(message = "Business name is required") String name,
        String phone,
        String address,
        String gstin,
        String stateName,
        String materialRegion,
        @DecimalMin(value = "0", message = "GST rate cannot be negative")
        @DecimalMax(value = "100", message = "GST rate cannot exceed 100")
        BigDecimal defaultGstRate
) {
}
