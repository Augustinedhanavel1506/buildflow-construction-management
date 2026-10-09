package com.buildflow.labour.dto;

import com.buildflow.labour.entity.Worker;

import java.math.BigDecimal;

public record WorkerResponse(
        Long id,
        Long projectId,
        String name,
        String role,
        BigDecimal dailyRate,
        boolean active
) {
    public static WorkerResponse from(Worker worker) {
        return new WorkerResponse(
                worker.getId(),
                worker.getProject().getId(),
                worker.getName(),
                worker.getRole(),
                worker.getDailyRate(),
                worker.isActive()
        );
    }
}
