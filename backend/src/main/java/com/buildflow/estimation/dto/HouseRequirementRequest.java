package com.buildflow.estimation.dto;

import com.buildflow.estimation.entity.ConstructionGrade;
import com.buildflow.estimation.entity.RoofType;
import com.buildflow.estimation.entity.StructureType;
import com.buildflow.estimation.entity.WallMaterial;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record HouseRequirementRequest(
        @NotBlank(message = "Label is required") String label,
        String location,
        @Size(max = 100, message = "District is too long") String district,
        @NotNull(message = "Plot width is required") @Positive(message = "Plot width must be greater than zero") BigDecimal plotWidthFt,
        @NotNull(message = "Plot length is required") @Positive(message = "Plot length must be greater than zero") BigDecimal plotLengthFt,
        @NotNull(message = "Construction grade is required") ConstructionGrade constructionGrade,
        StructureType structureType,
        WallMaterial wallMaterial,
        RoofType roofType,
        @NotEmpty(message = "At least one floor is required") @Valid List<FloorRequirementRequest> floors
) {
}
