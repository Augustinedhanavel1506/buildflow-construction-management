package com.buildflow.estimation.dto;

import com.buildflow.estimation.entity.OpeningKind;
import com.buildflow.estimation.entity.PlanSide;
import com.buildflow.estimation.entity.RoomType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public final class PlanModels {

    private PlanModels() {
    }

    public record RoomDto(
            @NotBlank(message = "Room id is required") @Size(max = 40, message = "Room id is too long") String id,
            @NotNull(message = "Room type is required") RoomType type,
            @Size(max = 60, message = "Room name is too long") String name,
            @NotNull(message = "Room x is required") @PositiveOrZero(message = "Room x cannot be negative") BigDecimal x,
            @NotNull(message = "Room y is required") @PositiveOrZero(message = "Room y cannot be negative") BigDecimal y,
            @NotNull(message = "Room width is required") @Positive(message = "Room width must be greater than zero") BigDecimal widthFt,
            @NotNull(message = "Room depth is required") @Positive(message = "Room depth must be greater than zero") BigDecimal depthFt
    ) {
    }

    public record OpeningDto(
            @NotBlank(message = "Opening id is required") @Size(max = 40, message = "Opening id is too long") String id,
            @NotBlank(message = "Opening room is required") String roomId,
            @NotNull(message = "Opening kind is required") OpeningKind kind,
            @NotNull(message = "Opening side is required") PlanSide side,
            @NotNull(message = "Opening offset is required") @PositiveOrZero(message = "Opening offset cannot be negative") BigDecimal offsetFt,
            @NotNull(message = "Opening width is required") @Positive(message = "Opening width must be greater than zero") BigDecimal widthFt
    ) {
    }

    public record ColumnDto(
            @NotBlank(message = "Column id is required") @Size(max = 40, message = "Column id is too long") String id,
            @NotNull(message = "Column x is required") @PositiveOrZero(message = "Column x cannot be negative") BigDecimal x,
            @NotNull(message = "Column y is required") @PositiveOrZero(message = "Column y cannot be negative") BigDecimal y,
            @NotNull(message = "Column width is required") @Positive(message = "Column width must be greater than zero") BigDecimal widthFt,
            @NotNull(message = "Column depth is required") @Positive(message = "Column depth must be greater than zero") BigDecimal depthFt
    ) {
    }

    // Furniture and fittings: layout only, never part of the estimate. x and y are the centre, in feet.
    public record FurnitureDto(
            @NotBlank(message = "Furniture id is required") @Size(max = 40, message = "Furniture id is too long") String id,
            @NotBlank(message = "Furniture kind is required") @Size(max = 30, message = "Furniture kind is too long") String kind,
            @NotNull(message = "Furniture x is required") @PositiveOrZero(message = "Furniture x cannot be negative") BigDecimal x,
            @NotNull(message = "Furniture y is required") @PositiveOrZero(message = "Furniture y cannot be negative") BigDecimal y,
            @NotNull(message = "Furniture width is required") @Positive(message = "Furniture width must be greater than zero") BigDecimal widthFt,
            @NotNull(message = "Furniture depth is required") @Positive(message = "Furniture depth must be greater than zero") BigDecimal depthFt,
            @NotNull(message = "Furniture height is required") @Positive(message = "Furniture height must be greater than zero") BigDecimal heightFt,
            int rotationDeg,
            @Size(max = 9, message = "Furniture colour is too long") String colour
    ) {
    }

    // Beams are derived from the columns and the walls, never stored or edited directly.
    public record BeamDto(BigDecimal x1, BigDecimal y1, BigDecimal x2, BigDecimal y2) {
    }

    public record LayoutRequest(
            @NotNull(message = "Rooms are required") List<@Valid RoomDto> rooms,
            List<@Valid OpeningDto> openings,
            List<@Valid ColumnDto> columns,
            List<@Valid FurnitureDto> furniture
    ) {
        public LayoutRequest(List<RoomDto> rooms, List<OpeningDto> openings, List<ColumnDto> columns) {
            this(rooms, openings, columns, null);
        }
    }

    // Concrete and steel of this floor's frame: columns, beams and slab, plus footings on the ground
    // floor. Ground-floor beams are counted at plinth and roof level.
    public record StructureMetrics(
            int columnCount,
            BigDecimal beamLengthFt,
            BigDecimal concreteM3,
            BigDecimal steelKg
    ) {
    }

    // builtUpAreaSqft is the sum of the drawn rooms, which is what the estimator uses as the floor
    // area; boundingWidthFt x boundingDepthFt is the envelope that encloses them.
    public record Metrics(
            BigDecimal builtUpAreaSqft,
            BigDecimal roomsAreaSqft,
            BigDecimal boundingWidthFt,
            BigDecimal boundingDepthFt,
            int bedrooms,
            int bathrooms,
            int doors,
            int windows,
            BigDecimal externalWallFt,
            BigDecimal internalWallFt,
            BigDecimal wallAreaSqft,
            BigDecimal plasterAreaSqft
    ) {
    }

    public record FloorPlanResponse(
            int floorLevel,
            boolean saved,
            List<RoomDto> rooms,
            List<OpeningDto> openings,
            List<ColumnDto> columns,
            List<BeamDto> beams,
            List<FurnitureDto> furniture,
            Metrics metrics,
            StructureMetrics structure
    ) {
    }

    public record PlansResponse(
            BigDecimal plotWidthFt,
            BigDecimal plotLengthFt,
            List<FloorPlanResponse> floors
    ) {
    }
}
