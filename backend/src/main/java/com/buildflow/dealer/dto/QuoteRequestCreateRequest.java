package com.buildflow.dealer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record QuoteRequestCreateRequest(
        @NotNull(message = "Project is required") Long projectId,
        @NotBlank(message = "Title is required") String title,
        @NotEmpty(message = "Select at least one dealer") List<Long> dealerIds
) {
}
