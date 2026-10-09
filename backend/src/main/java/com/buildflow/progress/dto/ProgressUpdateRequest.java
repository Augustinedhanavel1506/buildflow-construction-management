package com.buildflow.progress.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ProgressUpdateRequest(
        @Min(value = 0, message = "Progress cannot be less than 0%") @Max(value = 100, message = "Progress cannot exceed 100%") int percentComplete
) {
}
