package com.buildflow.labour.dto;

import jakarta.validation.constraints.NotNull;

public record AttendanceEntryRequest(
        @NotNull(message = "Worker is required") Long workerId,
        @NotNull(message = "Presence must be specified") Boolean present
) {
}
