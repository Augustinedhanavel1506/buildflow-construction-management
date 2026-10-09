package com.buildflow.calculator.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public final class CalculatorModels {

    private CalculatorModels() {
    }

    // ---- Concrete and reinforcement for structural members ----

    public enum MixGrade {
        M15, M20, M25
    }

    public enum MemberType {
        FOOTING, COLUMN, BEAM, SLAB, OTHER
    }

    public record ConcreteMember(
            @NotBlank(message = "Member name is required") String name,
            @NotNull(message = "Member type is required") MemberType type,
            @NotNull(message = "Count is required") @Positive(message = "Count must be at least 1") Integer count,
            @NotNull(message = "Length is required") @Positive(message = "Length must be greater than zero") BigDecimal lengthM,
            @NotNull(message = "Width is required") @Positive(message = "Width must be greater than zero") BigDecimal widthM,
            @NotNull(message = "Depth is required") @Positive(message = "Depth must be greater than zero") BigDecimal depthM,
            @PositiveOrZero(message = "Steel percentage cannot be negative") @DecimalMax(value = "10", message = "Steel percentage cannot exceed 10%") BigDecimal steelPercent
    ) {
    }

    public record ConcreteRequest(
            @NotNull(message = "Mix grade is required") MixGrade grade,
            @PositiveOrZero(message = "Wastage cannot be negative") @DecimalMax(value = "50", message = "Wastage cannot exceed 50%") BigDecimal wastagePercent,
            @NotEmpty(message = "Add at least one member") List<@Valid ConcreteMember> members
    ) {
    }

    public record MemberResult(String name, String type, int count, BigDecimal volumeM3,
                               BigDecimal steelPercent, BigDecimal steelKg) {
    }

    public record ConcreteResult(
            String grade, String mixRatio, List<MemberResult> members,
            BigDecimal concreteM3, BigDecimal cementBags, BigDecimal sandM3, BigDecimal aggregateM3,
            BigDecimal steelKg, BigDecimal wastagePercent, List<String> assumptions
    ) {
    }

    // ---- Masonry ----

    public enum UnitType {
        RED_BRICK, AAC_BLOCK, SOLID_BLOCK
    }

    public record Wall(
            @NotBlank(message = "Wall name is required") String name,
            @NotNull(message = "Length is required") @Positive(message = "Length must be greater than zero") BigDecimal lengthM,
            @NotNull(message = "Height is required") @Positive(message = "Height must be greater than zero") BigDecimal heightM,
            @NotNull(message = "Thickness is required") @Positive(message = "Thickness must be greater than zero") BigDecimal thicknessM,
            @PositiveOrZero(message = "Openings cannot be negative") BigDecimal openingsSqm
    ) {
    }

    public record MasonryRequest(
            @NotNull(message = "Unit type is required") UnitType unitType,
            @Min(value = 3, message = "Mortar ratio must be between 1:3 and 1:8") @Max(value = 8, message = "Mortar ratio must be between 1:3 and 1:8") Integer mortarSandParts,
            @PositiveOrZero(message = "Wastage cannot be negative") @DecimalMax(value = "50", message = "Wastage cannot exceed 50%") BigDecimal wastagePercent,
            @NotEmpty(message = "Add at least one wall") List<@Valid Wall> walls
    ) {
    }

    public record WallResult(String name, BigDecimal grossAreaSqm, BigDecimal netAreaSqm,
                             BigDecimal netVolumeM3, BigDecimal units) {
    }

    public record MasonryResult(
            String unitType, int mortarSandParts, List<WallResult> walls,
            BigDecimal units, BigDecimal mortarDryM3, BigDecimal cementBags, BigDecimal sandM3,
            BigDecimal wastagePercent, List<String> assumptions
    ) {
    }

    // ---- Reinforcement bars ----

    public record Bar(
            @NotBlank(message = "Bar mark is required") String mark,
            @NotNull(message = "Diameter is required") @Min(value = 6, message = "Diameter must be 6 to 40 mm") @Max(value = 40, message = "Diameter must be 6 to 40 mm") Integer diameterMm,
            @NotNull(message = "Cut length is required") @Positive(message = "Cut length must be greater than zero") BigDecimal lengthM,
            @NotNull(message = "Number of bars is required") @Positive(message = "Number of bars must be at least 1") Integer nos
    ) {
    }

    public record SteelRequest(
            @PositiveOrZero(message = "Wastage cannot be negative") @DecimalMax(value = "50", message = "Wastage cannot exceed 50%") BigDecimal wastagePercent,
            @Positive(message = "Standard bar length must be greater than zero") BigDecimal standardBarLengthM,
            @NotEmpty(message = "Add at least one bar") List<@Valid Bar> bars
    ) {
    }

    public record BarResult(String mark, int diameterMm, BigDecimal totalLengthM, BigDecimal weightKg) {
    }

    public record DiameterTotal(int diameterMm, BigDecimal totalLengthM, BigDecimal weightKg, int standardBarsRequired) {
    }

    public record SteelResult(
            List<BarResult> bars, List<DiameterTotal> byDiameter,
            BigDecimal totalKg, BigDecimal totalKgWithWastage, BigDecimal wastagePercent, List<String> assumptions
    ) {
    }
}
