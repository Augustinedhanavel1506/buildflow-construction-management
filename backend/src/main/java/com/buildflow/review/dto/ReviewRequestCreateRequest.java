package com.buildflow.review.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequestCreateRequest(
        @NotNull(message = "Project is required") Long projectId,
        @NotNull(message = "Engineer is required") Long engineerId,
        @Size(max = 500, message = "Message is too long") String message
) {
}
