package com.buildflow.progress.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ProgressStageRequest(
        @NotBlank(message = "Stage name is required") String stageName,
        @Min(value = 0, message = "Progress cannot be less than 0%") @Max(value = 100, message = "Progress cannot exceed 100%") int percentComplete
) {
}
