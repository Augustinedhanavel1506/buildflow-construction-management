package com.buildflow.estimation.service;

import com.buildflow.estimation.dto.PlanModels.BeamDto;
import com.buildflow.estimation.dto.PlanModels.ColumnDto;
import com.buildflow.estimation.dto.PlanModels.RoomDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Suggests and measures a simple RCC frame for a drawn floor: columns at wall junctions, beams
 * along the walls between aligned columns, a slab over the rooms, and isolated footings on the
 * ground floor. A concept frame for estimating, not a structural design: member sizes, steel
 * percentages and spans are fixed assumptions an engineer is expected to replace.
 */
final class StructureGeometry {

    static final double STOREY_HEIGHT_FT = 10;
    static final double COLUMN_WIDTH_FT = 0.75;
    static final double COLUMN_DEPTH_FT = 1.0;
    static final double BEAM_WIDTH_FT = 0.75;
    static final double BEAM_DEPTH_FT = 1.0;
    static final double SLAB_THICKNESS_FT = 5.0 / 12.0;
    static final double FOOTING_SIDE_FT = 4;
    static final double FOOTING_DEPTH_FT = 1.25;
    static final double MAX_SPAN_FT = 15;

    private static final double FT3_TO_M3 = 0.0283168;
    private static final double STEEL_KG_PER_M3 = 7850;
    private static final double STEEL_FOOTING = 0.008;
    private static final double STEEL_COLUMN = 0.015;
    private static final double STEEL_BEAM = 0.015;
    private static final double STEEL_SLAB = 0.008;
    private static final double UNIT_FT = 0.5;

    record Quantities(int columns, double beamFt, double concreteM3, double steelKg) {
        static final Quantities NONE = new Quantities(0, 0, 0, 0);

        Quantities plus(Quantities other) {
            return new Quantities(columns + other.columns, beamFt + other.beamFt,
                    concreteM3 + other.concreteM3, steelKg + other.steelKg);
        }
    }

    private StructureGeometry() {
    }

    /**
     * Columns on a structural grid: grid lines run along the walls and are spaced no further apart than
     * MAX_SPAN_FT, and a column goes where two grid lines cross on a wall. Wall corners are always added,
     * so the building outline and any step in it stay supported. Needs far fewer columns than one per
     * room corner while keeping every span within the limit.
     */
    static List<ColumnDto> suggestGridColumns(List<RoomDto> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            return List.of();
        }
        Set<String> walls = wallEdges(rooms);
        java.util.TreeSet<Integer> candX = new java.util.TreeSet<>();
        java.util.TreeSet<Integer> candY = new java.util.TreeSet<>();
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (RoomDto room : rooms) {
            int x0 = units(room.x().doubleValue());
            int y0 = units(room.y().doubleValue());
            int x1 = x0 + units(room.widthFt().doubleValue());
            int y1 = y0 + units(room.depthFt().doubleValue());
            candX.add(x0);
            candX.add(x1);
            candY.add(y0);
            candY.add(y1);
            minX = Math.min(minX, x0);
            maxX = Math.max(maxX, x1);
            minY = Math.min(minY, y0);
            maxY = Math.max(maxY, y1);
        }
        List<Integer> xs = gridLines(minX, maxX, candX);
        List<Integer> ys = gridLines(minY, maxY, candY);

        Map<String, int[]> points = new LinkedHashMap<>();
        // Grid crossings only where the long outer lines need a support between corners.
        java.util.Set<Integer> outerX = java.util.Set.of(minX, maxX);
        java.util.Set<Integer> outerY = java.util.Set.of(minY, maxY);
        for (int x : xs) {
            for (int y : ys) {
                boolean onOuterLine = outerX.contains(x) || outerY.contains(y);
                if (onOuterLine && touchesWall(walls, x, y) && isJunction(walls, x, y)) {
                    points.put(x + ":" + y, new int[]{x, y});
                }
            }
        }
        // Wall corners: exactly one horizontal and one vertical wall piece meet.
        for (int[] p : cornerPoints(walls, candX, candY)) {
            points.putIfAbsent(p[0] + ":" + p[1], p);
        }

        repairSpans(points, walls);

        List<ColumnDto> columns = new ArrayList<>();
        int n = 1;
        for (int[] p : points.values()) {
            columns.add(new ColumnDto("c" + n++, dec(p[0] * UNIT_FT), dec(p[1] * UNIT_FT),
                    dec(COLUMN_WIDTH_FT), dec(COLUMN_DEPTH_FT)));
        }
        return columns;
    }

    // Where two neighbouring columns on a wall line are further apart than MAX_SPAN_FT, add equally
    // spaced ones between them. A grid line that misses a wall at some crossing leaves such gaps.
    private static void repairSpans(Map<String, int[]> points, Set<String> walls) {
        int maxUnits = (int) Math.round(MAX_SPAN_FT / UNIT_FT);
        List<int[]> extra = new ArrayList<>();
        for (boolean horizontal : new boolean[]{true, false}) {
            Map<Integer, List<int[]>> lines = new TreeMap<>();
            for (int[] p : points.values()) {
                lines.computeIfAbsent(horizontal ? p[1] : p[0], k -> new ArrayList<>()).add(p);
            }
            for (List<int[]> line : lines.values()) {
                line.sort(Comparator.comparingInt(p -> horizontal ? p[0] : p[1]));
                for (int i = 0; i + 1 < line.size(); i++) {
                    int[] a = line.get(i);
                    int[] b = line.get(i + 1);
                    int gap = horizontal ? b[0] - a[0] : b[1] - a[1];
                    if (gap > maxUnits && onWall(walls, a, b, horizontal)) {
                        int pieces = (int) Math.ceil((double) gap / maxUnits);
                        for (int k = 1; k < pieces; k++) {
                            int offset = (int) Math.round((double) gap * k / pieces);
                            extra.add(horizontal ? new int[]{a[0] + offset, a[1]} : new int[]{a[0], a[1] + offset});
                        }
                    }
                }
            }
        }
        for (int[] p : extra) {
            points.putIfAbsent(p[0] + ":" + p[1], p);
        }
    }

    // Grid lines from min to max, each at most MAX_SPAN_FT from the last, snapped to a wall line when one is near.
    private static List<Integer> gridLines(int min, int max, java.util.NavigableSet<Integer> candidates) {
        int maxUnits = (int) Math.round(MAX_SPAN_FT / UNIT_FT);
        List<Integer> lines = new ArrayList<>();
        lines.add(min);
        int last = min;
        while (max - last > maxUnits) {
            int intervals = (int) Math.ceil((double) (max - last) / maxUnits);
            int ideal = last + (int) Math.round((double) (max - last) / intervals);
            Integer best = null;
            for (int c : candidates.subSet(last + 1, true, last + maxUnits, true)) {
                if (best == null || Math.abs(c - ideal) < Math.abs(best - ideal)) {
                    best = c;
                }
            }
            if (best != null) {
                last = best;
                lines.add(last);
            } else {
                // No wall line in reach: step over it; repairSpans adds columns along any wall that needs them.
                last = Math.min(ideal, last + maxUnits);
            }
        }
        if (last != max) {
            lines.add(max);
        }
        return lines;
    }

    // A point where an interior wall meets (or crosses) a wall line, not a plain straight run.
    private static boolean isJunction(Set<String> walls, int x, int y) {
        int h = (walls.contains("H:" + y + ":" + (x - 1)) ? 1 : 0) + (walls.contains("H:" + y + ":" + x) ? 1 : 0);
        int v = (walls.contains("V:" + x + ":" + (y - 1)) ? 1 : 0) + (walls.contains("V:" + x + ":" + y) ? 1 : 0);
        return h + v >= 3;
    }

    private static boolean touchesWall(Set<String> walls, int x, int y) {
        return walls.contains("H:" + y + ":" + (x - 1)) || walls.contains("H:" + y + ":" + x)
                || walls.contains("V:" + x + ":" + (y - 1)) || walls.contains("V:" + x + ":" + y);
    }

    private static List<int[]> cornerPoints(Set<String> walls, java.util.NavigableSet<Integer> xs, java.util.NavigableSet<Integer> ys) {
        List<int[]> corners = new ArrayList<>();
        for (int x : xs) {
            for (int y : ys) {
                int horizontal = (walls.contains("H:" + y + ":" + (x - 1)) ? 1 : 0) + (walls.contains("H:" + y + ":" + x) ? 1 : 0);
                int vertical = (walls.contains("V:" + x + ":" + (y - 1)) ? 1 : 0) + (walls.contains("V:" + x + ":" + y) ? 1 : 0);
                if (horizontal == 1 && vertical == 1) {
                    corners.add(new int[]{x, y});
                }
            }
        }
        return corners;
    }

    /** Columns at every distinct room corner, plus intermediate ones where a wall spans too far. */
    static List<ColumnDto> suggestColumns(List<RoomDto> rooms) {
        Map<String, int[]> points = new LinkedHashMap<>();
        for (RoomDto room : rooms) {
            int x0 = units(room.x().doubleValue());
            int y0 = units(room.y().doubleValue());
            int x1 = x0 + units(room.widthFt().doubleValue());
            int y1 = y0 + units(room.depthFt().doubleValue());
            for (int[] p : new int[][]{{x0, y0}, {x1, y0}, {x0, y1}, {x1, y1}}) {
                points.putIfAbsent(p[0] + ":" + p[1], p);
            }
        }
        Set<String> walls = wallEdges(rooms);

        // Intermediate columns along any wall line whose neighbouring columns are further apart than MAX_SPAN_FT.
        List<int[]> all = new ArrayList<>(points.values());
        List<int[]> extra = new ArrayList<>();
        int maxUnits = (int) Math.round(MAX_SPAN_FT / UNIT_FT);
        for (boolean horizontal : new boolean[]{true, false}) {
            Map<Integer, List<int[]>> lines = new TreeMap<>();
            for (int[] p : all) {
                lines.computeIfAbsent(horizontal ? p[1] : p[0], k -> new ArrayList<>()).add(p);
            }
            for (List<int[]> line : lines.values()) {
                line.sort(Comparator.comparingInt(p -> horizontal ? p[0] : p[1]));
                for (int i = 0; i + 1 < line.size(); i++) {
                    int[] a = line.get(i);
                    int[] b = line.get(i + 1);
                    int gap = horizontal ? b[0] - a[0] : b[1] - a[1];
                    if (gap > maxUnits && onWall(walls, a, b, horizontal)) {
                        int pieces = (int) Math.ceil((double) gap / maxUnits);
                        for (int k = 1; k < pieces; k++) {
                            int offset = (int) Math.round((double) gap * k / pieces);
                            extra.add(horizontal ? new int[]{a[0] + offset, a[1]} : new int[]{a[0], a[1] + offset});
                        }
                    }
                }
            }
        }
        for (int[] p : extra) {
            points.putIfAbsent(p[0] + ":" + p[1], p);
        }

        List<ColumnDto> columns = new ArrayList<>();
        int n = 1;
        for (int[] p : points.values()) {
            columns.add(new ColumnDto("c" + n++, dec(p[0] * UNIT_FT), dec(p[1] * UNIT_FT),
                    dec(COLUMN_WIDTH_FT), dec(COLUMN_DEPTH_FT)));
        }
        return columns;
    }

    /** Beams between consecutive aligned columns, only where a drawn wall actually runs between them. */
    static List<BeamDto> beams(List<ColumnDto> columns, List<RoomDto> rooms) {
        Set<String> walls = wallEdges(rooms);
        List<BeamDto> beams = new ArrayList<>();
        for (boolean horizontal : new boolean[]{true, false}) {
            Map<Integer, List<int[]>> lines = new TreeMap<>();
            for (ColumnDto c : columns) {
                int[] p = {units(c.x().doubleValue()), units(c.y().doubleValue())};
                lines.computeIfAbsent(horizontal ? p[1] : p[0], k -> new ArrayList<>()).add(p);
            }
            for (List<int[]> line : lines.values()) {
                line.sort(Comparator.comparingInt(p -> horizontal ? p[0] : p[1]));
                for (int i = 0; i + 1 < line.size(); i++) {
                    int[] a = line.get(i);
                    int[] b = line.get(i + 1);
                    if (a[0] == b[0] && a[1] == b[1]) continue;
                    if (onWall(walls, a, b, horizontal)) {
                        beams.add(new BeamDto(dec(a[0] * UNIT_FT), dec(a[1] * UNIT_FT), dec(b[0] * UNIT_FT), dec(b[1] * UNIT_FT)));
                    }
                }
            }
        }
        return beams;
    }

    static Quantities quantities(int floorLevel, List<ColumnDto> columns, List<RoomDto> rooms) {
        if (columns == null || columns.isEmpty()) {
            return Quantities.NONE;
        }
        double columnFt3 = 0;
        for (ColumnDto c : columns) {
            columnFt3 += c.widthFt().doubleValue() * c.depthFt().doubleValue() * STOREY_HEIGHT_FT;
        }
        double beamFt = 0;
        for (BeamDto b : beams(columns, rooms)) {
            beamFt += Math.abs(b.x2().doubleValue() - b.x1().doubleValue()) + Math.abs(b.y2().doubleValue() - b.y1().doubleValue());
        }
        // The ground floor carries a plinth beam as well as its roof-level beam.
        double beamLevels = floorLevel == 0 ? 2 : 1;
        double beamFt3 = beamFt * beamLevels * BEAM_WIDTH_FT * BEAM_DEPTH_FT;
        double roomsArea = rooms.stream().mapToDouble(r -> r.widthFt().doubleValue() * r.depthFt().doubleValue()).sum();
        double slabFt3 = roomsArea * SLAB_THICKNESS_FT;
        double footingFt3 = floorLevel == 0 ? columns.size() * FOOTING_SIDE_FT * FOOTING_SIDE_FT * FOOTING_DEPTH_FT : 0;

        double concrete = (columnFt3 + beamFt3 + slabFt3 + footingFt3) * FT3_TO_M3;
        double steel = (footingFt3 * STEEL_FOOTING + columnFt3 * STEEL_COLUMN + beamFt3 * STEEL_BEAM + slabFt3 * STEEL_SLAB)
                * FT3_TO_M3 * STEEL_KG_PER_M3;
        return new Quantities(columns.size(), beamFt, concrete, steel);
    }

    // Half-foot wall pieces used by any room, keyed like "H:y:x" (a horizontal piece) or "V:x:y".
    private static Set<String> wallEdges(List<RoomDto> rooms) {
        Set<String> edges = new HashSet<>();
        for (RoomDto room : rooms) {
            int x0 = units(room.x().doubleValue());
            int y0 = units(room.y().doubleValue());
            int x1 = x0 + units(room.widthFt().doubleValue());
            int y1 = y0 + units(room.depthFt().doubleValue());
            for (int x = x0; x < x1; x++) {
                edges.add("H:" + y0 + ":" + x);
                edges.add("H:" + y1 + ":" + x);
            }
            for (int y = y0; y < y1; y++) {
                edges.add("V:" + x0 + ":" + y);
                edges.add("V:" + x1 + ":" + y);
            }
        }
        return edges;
    }

    private static boolean onWall(Set<String> walls, int[] a, int[] b, boolean horizontal) {
        if (horizontal) {
            for (int x = Math.min(a[0], b[0]); x < Math.max(a[0], b[0]); x++) {
                if (!walls.contains("H:" + a[1] + ":" + x)) return false;
            }
        } else {
            for (int y = Math.min(a[1], b[1]); y < Math.max(a[1], b[1]); y++) {
                if (!walls.contains("V:" + a[0] + ":" + y)) return false;
            }
        }
        return true;
    }

    private static int units(double feet) {
        return (int) Math.round(feet / UNIT_FT);
    }

    private static BigDecimal dec(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
