package com.buildflow.dailyreport.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DailyReportRequest(
        @NotNull(message = "Report date is required") LocalDate reportDate,
        @PositiveOrZero(message = "Workers present cannot be negative") Integer workersPresent,
        @Size(max = 2000, message = "Work completed is too long") String workCompleted,
        @Size(max = 2000, message = "Materials used is too long") String materialsUsed,
        @Size(max = 2000, message = "Issues is too long") String issues,
        @Size(max = 2000, message = "Notes is too long") String notes
) {
}
