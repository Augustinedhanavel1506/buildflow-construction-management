package com.buildflow.estimation.service;

import com.buildflow.estimation.dto.PlanModels.LayoutRequest;
import com.buildflow.estimation.dto.PlanModels.OpeningDto;
import com.buildflow.estimation.dto.PlanModels.RoomDto;
import com.buildflow.estimation.entity.OpeningKind;
import com.buildflow.estimation.entity.PlanSide;
import com.buildflow.estimation.entity.RoomType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Proposes a concept layout for one floor from its room checklist. Rooms are grouped into bands
 * across the plot (public rooms at the road-facing front, bedrooms and kitchen in the middle,
 * staircase and bathrooms at the rear). It is a starting point to edit, not a design: no vastu,
 * setback rules beyond a simple margin, or structural grid is applied.
 *
 * Every floor of a multi-storey house shares one footprint (sized from its largest floor) and the
 * staircase is anchored at the rear of it, so the stair lines up from floor to floor. A smaller
 * upper floor leaves part of that footprint open, as a terrace, rather than shifting the stair.
 */
final class LayoutSuggester {

    record Spec(double areaSqft, double footprintAreaSqft, int bedrooms, int bathrooms, boolean kitchen,
                boolean hall, boolean balcony, boolean pooja, boolean multiFloor,
                double frontSetback, double rearSetback, double leftSetback, double rightSetback) {
    }

    private record RoomSpec(RoomType type, String name, double target) {
    }

    private static final class Band {
        final List<RoomSpec> rooms = new ArrayList<>();
        boolean anchoredRear;
        double height;

        double weight() {
            return rooms.stream().mapToDouble(RoomSpec::target).sum();
        }
    }

    private static final double MIN_BAND_HEIGHT = 7;
    private static final double STAIR_WIDTH = 7;
    private static final double STAIR_BAND_HEIGHT = 10;
    private static final double EPS = 0.01;

    private LayoutSuggester() {
    }

    static LayoutRequest suggest(double plotWidth, double plotLength, Spec spec) {
        if (buildBands(spec, 3).isEmpty()) {
            return new LayoutRequest(List.of(), List.of(), List.of());
        }

        double ux = spec.leftSetback();
        double uy = spec.frontSetback();
        double uw = plotWidth - spec.leftSetback() - spec.rightSetback();
        double ul = plotLength - spec.frontSetback() - spec.rearSetback();
        if (uw < 10 || ul < 10) {
            ux = 0;
            uy = 0;
            uw = plotWidth;
            ul = plotLength;
        }

        // Footprint: shrink the usable area (keeping its proportions) toward the largest floor.
        double footprint = spec.footprintAreaSqft() > 0 ? spec.footprintAreaSqft() : spec.areaSqft();
        double ratio = footprint > 0 ? Math.min(1, Math.sqrt(footprint / (uw * ul))) : 1;
        double width = round05(ratio < 1 ? Math.max(uw * ratio, Math.min(10, uw)) : uw);
        double depth = round05(ratio < 1 ? Math.max(ul * ratio, Math.min(10, ul)) : ul);

        double rearHeight = spec.multiFloor() ? Math.min(STAIR_BAND_HEIGHT, round05(depth * 0.4)) : 0;
        double available = depth - rearHeight;

        // Try packing the middle rooms 3 or 2 to a band and keep whichever leaves rooms least stretched.
        List<Band> bands = null;
        double bestStretch = Double.MAX_VALUE;
        for (int chunk : new int[]{3, 2}) {
            List<Band> candidate = buildBands(spec, chunk);
            List<Band> candidateStack = candidate.stream().filter(b -> !b.anchoredRear).toList();
            allocateHeights(candidateStack, round05(available));
            double stretch = worstStretch(candidateStack, width);
            if (stretch < bestStretch - 0.05) {
                bestStretch = stretch;
                bands = candidate;
            }
        }
        Band rear = bands.stream().filter(b -> b.anchoredRear).findFirst().orElse(null);
        List<Band> stack = bands.stream().filter(b -> !b.anchoredRear).toList();

        // A floor smaller than the footprint fills only as much height as its area needs.
        double stackHeight = available;
        if (rear != null && spec.areaSqft() > 0 && spec.areaSqft() < footprint * 0.98 && !stack.isEmpty()) {
            double needed = (spec.areaSqft() - width * rearHeight) / width;
            stackHeight = Math.min(available, Math.max(needed, MIN_BAND_HEIGHT * stack.size()));
        }
        stackHeight = round05(stackHeight);
        allocateHeights(stack, stackHeight);
        if (rear != null) {
            rear.height = rearHeight;
        }

        List<RoomDto> rooms = new ArrayList<>();
        List<OpeningDto> openings = new ArrayList<>();
        int[] counters = {1, 1};
        double y = uy;
        for (int b = 0; b < stack.size(); b++) {
            Band band = stack.get(b);
            double bandHeight = b == stack.size() - 1 ? round05(uy + stackHeight - y) : band.height;
            placeBand(band, ux, y, width, bandHeight, uy, depth, b == 0, rooms, openings, counters);
            y += bandHeight;
        }
        if (rear != null) {
            placeBand(rear, ux, uy + depth - rearHeight, width, rearHeight, uy, depth, stack.isEmpty(), rooms, openings, counters);
        }
        return new LayoutRequest(rooms, openings, List.of());
    }

    // Largest length-to-width ratio any room in the stack would get at full-height allocation.
    private static double worstStretch(List<Band> stack, double width) {
        double worst = 1;
        for (Band band : stack) {
            for (double w : bandWidths(band, width)) {
                double ratio = Math.max(w, band.height) / Math.max(Math.min(w, band.height), 0.1);
                worst = Math.max(worst, ratio);
            }
        }
        return worst;
    }

    private static void placeBand(Band band, double ux, double y, double width, double bandHeight,
                                   double uy, double depth, boolean frontBand,
                                   List<RoomDto> rooms, List<OpeningDto> openings, int[] counters) {
        double x = ux;
        double bandRight = ux + width;
        List<Double> widths = bandWidths(band, width);
        for (int i = 0; i < band.rooms.size(); i++) {
            RoomSpec room = band.rooms.get(i);
            double w = widths.get(i);
            // The last room absorbs rounding so the band ends flush with the envelope; a band
            // holding only the staircase keeps its fixed width.
            boolean stairOnly = band.rooms.size() == 1 && room.type() == RoomType.STAIRCASE;
            if (i == band.rooms.size() - 1 && !stairOnly) {
                w = round05(bandRight - x);
            }
            String id = "r" + counters[0]++;
            rooms.add(new RoomDto(id, room.type(), room.name(), dec(x), dec(y), dec(w), dec(bandHeight)));

            boolean north = Math.abs(y - uy) < EPS;
            boolean south = Math.abs(y + bandHeight - (uy + depth)) < EPS;
            boolean west = Math.abs(x - ux) < EPS;
            boolean east = Math.abs(x + w - bandRight) < EPS;

            // The hall is the main entrance on the front; every other room opens toward the rest of the house.
            PlanSide doorSide = (frontBand && room.type() == RoomType.HALL) ? PlanSide.NORTH
                    : frontBand ? PlanSide.SOUTH : PlanSide.NORTH;
            double doorWidth = room.type() == RoomType.BATHROOM || room.type() == RoomType.POOJA ? 2.5 : 3.0;
            OpeningDto door = opening(counters[1], id, OpeningKind.DOOR, doorSide, w, bandHeight, doorWidth);
            if (door != null) {
                openings.add(door);
                counters[1]++;
            }

            // Windows on outer walls, never on a side that already carries the door.
            if (room.type() != RoomType.STAIRCASE) {
                double windowWidth = room.type() == RoomType.BATHROOM ? 2.0 : 3.5;
                PlanSide[] outer = {north ? PlanSide.NORTH : null, east ? PlanSide.EAST : null,
                        south ? PlanSide.SOUTH : null, west ? PlanSide.WEST : null};
                for (PlanSide side : outer) {
                    if (side == null || side == doorSide) {
                        continue;
                    }
                    OpeningDto window = opening(counters[1], id, OpeningKind.WINDOW, side, w, bandHeight, windowWidth);
                    if (window != null) {
                        openings.add(window);
                        counters[1]++;
                    }
                }
            }
            x += w;
        }
    }

    private static OpeningDto opening(int counter, String roomId, OpeningKind kind, PlanSide side,
                                       double roomWidth, double roomDepth, double width) {
        double sideLength = (side == PlanSide.NORTH || side == PlanSide.SOUTH) ? roomWidth : roomDepth;
        if (sideLength < width + 1) {
            return null;
        }
        double offset = round05((sideLength - width) / 2);
        return new OpeningDto("o" + counter, roomId, kind, side, dec(offset), dec(width));
    }

    // Bands in plan order from the front. The staircase band, when there is one, comes last and is
    // anchored to the rear of the footprint.
    private static List<Band> buildBands(Spec spec, int middleChunk) {
        List<RoomSpec> front = new ArrayList<>();
        if (spec.hall()) front.add(new RoomSpec(RoomType.HALL, "Living Hall", 180));
        if (spec.balcony()) front.add(new RoomSpec(RoomType.BALCONY, "Balcony", 50));
        if (spec.pooja()) front.add(new RoomSpec(RoomType.POOJA, "Pooja Room", 25));

        List<RoomSpec> middle = new ArrayList<>();
        if (spec.kitchen()) middle.add(new RoomSpec(RoomType.KITCHEN, "Kitchen", 100));
        for (int i = 1; i <= spec.bedrooms(); i++) {
            middle.add(new RoomSpec(RoomType.BEDROOM, "Bedroom " + i, i == 1 ? 150 : 120));
        }

        List<RoomSpec> rear = new ArrayList<>();
        if (spec.multiFloor()) rear.add(new RoomSpec(RoomType.STAIRCASE, "Staircase", 70));
        for (int i = 1; i <= spec.bathrooms(); i++) {
            rear.add(new RoomSpec(RoomType.BATHROOM, "Bath " + i, 40));
        }

        List<Band> bands = new ArrayList<>();
        addChunked(bands, front, 3);
        addChunked(bands, middle, middleChunk);

        List<Band> rearBands = new ArrayList<>();
        addChunked(rearBands, rear, 4);
        if (!rearBands.isEmpty()) {
            Band first = rearBands.get(0);
            boolean hasStair = first.rooms.get(0).type() == RoomType.STAIRCASE;
            // Extra bathroom bands sit before the stair band so the stair stays at the very rear.
            bands.addAll(rearBands.subList(1, rearBands.size()));
            if (hasStair) {
                first.anchoredRear = true;
            }
            bands.add(first);
        }
        return bands;
    }

    private static void addChunked(List<Band> bands, List<RoomSpec> rooms, int max) {
        for (int i = 0; i < rooms.size(); i += max) {
            Band band = new Band();
            band.rooms.addAll(rooms.subList(i, Math.min(i + max, rooms.size())));
            bands.add(band);
        }
    }

    // Splits `total` height across bands in proportion to their room areas, with a minimum height.
    private static void allocateHeights(List<Band> bands, double total) {
        if (bands.isEmpty()) {
            return;
        }
        double weightTotal = bands.stream().mapToDouble(Band::weight).sum();
        boolean[] locked = new boolean[bands.size()];
        for (Band band : bands) {
            band.height = total * band.weight() / weightTotal;
        }
        for (int pass = 0; pass < 3; pass++) {
            double deficit = 0;
            double flex = 0;
            for (int i = 0; i < bands.size(); i++) {
                Band band = bands.get(i);
                if (band.height < MIN_BAND_HEIGHT) {
                    deficit += MIN_BAND_HEIGHT - band.height;
                    band.height = MIN_BAND_HEIGHT;
                    locked[i] = true;
                } else if (!locked[i]) {
                    flex += band.height;
                }
            }
            if (deficit <= 0 || flex <= 0) break;
            for (int i = 0; i < bands.size(); i++) {
                if (!locked[i]) bands.get(i).height -= deficit * bands.get(i).height / flex;
            }
        }
        // Whole half-feet so rooms snap to the editor grid; the last band absorbs any remainder.
        for (Band band : bands) {
            band.height = round05(band.height);
        }
    }

    private static List<Double> bandWidths(Band band, double totalWidth) {
        List<Double> widths = new ArrayList<>();
        if (band.rooms.get(0).type() == RoomType.STAIRCASE) {
            widths.add(STAIR_WIDTH);
            if (band.rooms.size() > 1) {
                double rest = totalWidth - STAIR_WIDTH;
                double weight = band.rooms.stream().skip(1).mapToDouble(RoomSpec::target).sum();
                for (int i = 1; i < band.rooms.size(); i++) {
                    widths.add(round05(rest * band.rooms.get(i).target() / weight));
                }
            }
            return widths;
        }
        double weight = band.weight();
        for (RoomSpec room : band.rooms) {
            widths.add(round05(totalWidth * room.target() / weight));
        }
        return widths;
    }

    private static double round05(double value) {
        return Math.round(value * 2) / 2.0;
    }

    private static BigDecimal dec(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
