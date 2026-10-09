package com.buildflow.labour.dto;

import com.buildflow.labour.entity.Worker;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AttendanceEntryResponse(
        Long workerId,
        String workerName,
        String role,
        BigDecimal dailyRate,
        LocalDate date,
        boolean present
) {
    public static AttendanceEntryResponse of(Worker worker, LocalDate date, boolean present) {
        return new AttendanceEntryResponse(
                worker.getId(),
                worker.getName(),
                worker.getRole(),
                worker.getDailyRate(),
                date,
                present
        );
    }
}
