package com.buildflow.project.dto;

import com.buildflow.project.entity.Project;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ProjectResponse(
        Long id,
        String name,
        String clientName,
        String clientGstin,
        String clientAddress,
        String location,
        BigDecimal contractValue,
        BigDecimal revisedContractValue,
        BigDecimal estimatedCost,
        BigDecimal actualCost,
        int overallProgress,
        LocalDate startDate,
        LocalDate expectedEndDate,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProjectResponse from(
            Project project, BigDecimal actualCost, double overallProgress, BigDecimal netApprovedVariations) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getClientName(),
                project.getClientGstin(),
                project.getClientAddress(),
                project.getLocation(),
                project.getContractValue(),
                project.getContractValue().add(netApprovedVariations),
                project.getEstimatedCost(),
                actualCost,
                (int) Math.round(overallProgress),
                project.getStartDate(),
                project.getExpectedEndDate(),
                project.getStatus().name(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
