package com.buildflow.estimation.service;

import com.buildflow.estimation.dto.PlanModels.BeamDto;
import com.buildflow.estimation.dto.PlanModels.ColumnDto;
import com.buildflow.estimation.dto.PlanModels.FloorPlanResponse;
import com.buildflow.estimation.dto.PlanModels.FurnitureDto;
import com.buildflow.estimation.dto.PlanModels.OpeningDto;
import com.buildflow.estimation.dto.PlanModels.PlansResponse;
import com.buildflow.estimation.dto.PlanModels.RoomDto;

import java.util.Locale;
import java.util.Map;

/**
 * Writes the drawn plans as an ASCII DXF (R12) that AutoCAD, BricsCAD, LibreCAD and similar tools open.
 * Units are feet. Floors sit side by side, ground floor on the left, with the plot outline around each.
 * Plan y runs down the page, so it is flipped (y becomes -y) to keep the road-facing front at the top.
 * A concept drawing for coordination, not a construction drawing.
 */
final class DxfExporter {

    private static final double FLOOR_GAP_FT = 15;
    private static final Map<String, Integer> LAYERS = new java.util.LinkedHashMap<>();

    static {
        LAYERS.put("PLOT", 8);
        LAYERS.put("WALLS", 7);
        LAYERS.put("DOORS", 3);
        LAYERS.put("WINDOWS", 5);
        LAYERS.put("COLUMNS", 1);
        LAYERS.put("FURNITURE", 4);
        LAYERS.put("BEAMS", 6);
        LAYERS.put("TEXT", 2);
    }

    private DxfExporter() {
    }

    static String export(PlansResponse plans) {
        StringBuilder out = new StringBuilder();
        pair(out, 0, "SECTION");
        pair(out, 2, "HEADER");
        pair(out, 9, "$ACADVER");
        pair(out, 1, "AC1009");
        pair(out, 9, "$INSUNITS");
        pair(out, 70, "2");
        pair(out, 0, "ENDSEC");

        pair(out, 0, "SECTION");
        pair(out, 2, "TABLES");
        pair(out, 0, "TABLE");
        pair(out, 2, "LAYER");
        pair(out, 70, String.valueOf(LAYERS.size()));
        LAYERS.forEach((name, colour) -> {
            pair(out, 0, "LAYER");
            pair(out, 2, name);
            pair(out, 70, "0");
            pair(out, 62, String.valueOf(colour));
            pair(out, 6, "CONTINUOUS");
        });
        pair(out, 0, "ENDTAB");
        pair(out, 0, "ENDSEC");

        pair(out, 0, "SECTION");
        pair(out, 2, "ENTITIES");
        double plotWidth = plans.plotWidthFt().doubleValue();
        double plotLength = plans.plotLengthFt().doubleValue();
        double offsetX = 0;
        for (FloorPlanResponse floor : plans.floors()) {
            rectangle(out, "PLOT", offsetX, 0, plotWidth, plotLength);
            text(out, offsetX, 2, floor.floorLevel() == 0 ? "GROUND FLOOR" : "FLOOR " + floor.floorLevel(), 1.2);
            for (RoomDto room : floor.rooms()) {
                double x = room.x().doubleValue() + offsetX;
                double y = room.y().doubleValue();
                double w = room.widthFt().doubleValue();
                double d = room.depthFt().doubleValue();
                rectangle(out, "WALLS", x, y, w, d);
                String label = (room.name() == null || room.name().isBlank() ? room.type().name() : room.name())
                        + String.format(Locale.ROOT, " %.1fx%.1f", w, d);
                text(out, x + 0.5, y + d / 2, label, Math.min(0.8, Math.max(0.35, w / Math.max(label.length(), 1) * 1.4)));
            }
            final double floorOffset = offsetX;
            for (OpeningDto opening : floor.openings()) {
                floor.rooms().stream().filter(r -> r.id().equals(opening.roomId())).findFirst()
                        .ifPresent(room -> opening(out, room, opening, floorOffset));
            }
            for (BeamDto beam : floor.beams()) {
                line(out, "BEAMS", beam.x1().doubleValue() + offsetX, beam.y1().doubleValue(),
                        beam.x2().doubleValue() + offsetX, beam.y2().doubleValue());
            }
            for (FurnitureDto item : floor.furniture()) {
                boolean turned = item.rotationDeg() == 90 || item.rotationDeg() == 270;
                double w = (turned ? item.depthFt() : item.widthFt()).doubleValue();
                double d = (turned ? item.widthFt() : item.depthFt()).doubleValue();
                rectangle(out, "FURNITURE", item.x().doubleValue() + offsetX - w / 2, item.y().doubleValue() - d / 2, w, d);
                text(out, item.x().doubleValue() + offsetX - w / 2 + 0.2, item.y().doubleValue(), item.kind(), 0.3);
            }
            for (ColumnDto column : floor.columns()) {
                double w = column.widthFt().doubleValue();
                double d = column.depthFt().doubleValue();
                rectangle(out, "COLUMNS", column.x().doubleValue() + offsetX - w / 2, column.y().doubleValue() - d / 2, w, d);
            }
            offsetX += plotWidth + FLOOR_GAP_FT;
        }
        pair(out, 0, "ENDSEC");
        pair(out, 0, "EOF");
        return out.toString();
    }

    private static void opening(StringBuilder out, RoomDto room, OpeningDto opening, double offsetX) {
        double x = room.x().doubleValue() + offsetX;
        double y = room.y().doubleValue();
        double w = room.widthFt().doubleValue();
        double d = room.depthFt().doubleValue();
        double at = opening.offsetFt().doubleValue();
        double len = opening.widthFt().doubleValue();
        String layer = opening.kind().name().equals("DOOR") ? "DOORS" : "WINDOWS";
        switch (opening.side()) {
            case NORTH -> line(out, layer, x + at, y, x + at + len, y);
            case SOUTH -> line(out, layer, x + at, y + d, x + at + len, y + d);
            case WEST -> line(out, layer, x, y + at, x, y + at + len);
            case EAST -> line(out, layer, x + w, y + at, x + w, y + at + len);
        }
    }

    private static void rectangle(StringBuilder out, String layer, double x, double y, double w, double d) {
        line(out, layer, x, y, x + w, y);
        line(out, layer, x + w, y, x + w, y + d);
        line(out, layer, x + w, y + d, x, y + d);
        line(out, layer, x, y + d, x, y);
    }

    private static void line(StringBuilder out, String layer, double x1, double y1, double x2, double y2) {
        pair(out, 0, "LINE");
        pair(out, 8, layer);
        pair(out, 10, num(x1));
        pair(out, 20, num(-y1));
        pair(out, 30, "0.0");
        pair(out, 11, num(x2));
        pair(out, 21, num(-y2));
        pair(out, 31, "0.0");
    }

    private static void text(StringBuilder out, double x, double y, String value, double height) {
        pair(out, 0, "TEXT");
        pair(out, 8, "TEXT");
        pair(out, 10, num(x));
        pair(out, 20, num(-y));
        pair(out, 30, "0.0");
        pair(out, 40, num(height));
        pair(out, 1, value.replaceAll("[\\r\\n]", " "));
    }

    private static String num(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static void pair(StringBuilder out, int code, String value) {
        out.append(code).append('\n').append(value).append('\n');
    }
}
