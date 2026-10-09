package com.buildflow.estimation.dto;

import com.buildflow.estimation.entity.HouseRequirement;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record HouseRequirementResponse(
        Long id,
        Long projectId,
        String label,
        String location,
        String district,
        BigDecimal plotWidthFt,
        BigDecimal plotLengthFt,
        String constructionGrade,
        String structureType,
        String wallMaterial,
        String roofType,
        String status,
        List<FloorRequirementResponse> floors,
        Instant createdAt
) {
    public static HouseRequirementResponse from(HouseRequirement requirement) {
        return new HouseRequirementResponse(
                requirement.getId(),
                requirement.getProject().getId(),
                requirement.getLabel(),
                requirement.getLocation(),
                requirement.getDistrict(),
                requirement.getPlotWidthFt(),
                requirement.getPlotLengthFt(),
                requirement.getConstructionGrade().name(),
                requirement.getStructureType().name(),
                requirement.getWallMaterial().name(),
                requirement.getRoofType().name(),
                requirement.getStatus().name(),
                requirement.getFloors().stream().map(FloorRequirementResponse::from).toList(),
                requirement.getCreatedAt()
        );
    }
}
