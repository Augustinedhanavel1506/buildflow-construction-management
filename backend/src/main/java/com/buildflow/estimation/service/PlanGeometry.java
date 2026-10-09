package com.buildflow.estimation.service;

import com.buildflow.estimation.dto.PlanModels.OpeningDto;
import com.buildflow.estimation.dto.PlanModels.RoomDto;
import com.buildflow.estimation.entity.OpeningKind;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a drawn floor into the quantities an estimate needs: wall lengths and areas, plaster and
 * paint area. Rooms are treated as drawn to wall centres on a half-foot grid, so a wall shared by
 * two rooms is counted once and any wall bordering a single room is an external wall.
 *
 * These are estimating approximations, not a quantity survey: floor height and opening heights
 * are fixed assumptions, wall thickness is applied later through rule coefficients, and openings
 * are deducted at face level only.
 */
final class PlanGeometry {

    static final double FLOOR_HEIGHT_FT = 10;
    static final double DOOR_HEIGHT_FT = 7;
    static final double WINDOW_HEIGHT_FT = 4;
    private static final double UNIT_FT = 0.5;

    record Derived(
            double externalWallFt,
            double internalWallFt,
            double externalWallAreaSqft,
            double internalWallAreaSqft,
            double plasterAreaSqft,
            double roomsAreaSqft
    ) {
        static final Derived NONE = new Derived(0, 0, 0, 0, 0, 0);

        Derived plus(Derived other) {
            return new Derived(
                    externalWallFt + other.externalWallFt, internalWallFt + other.internalWallFt,
                    externalWallAreaSqft + other.externalWallAreaSqft, internalWallAreaSqft + other.internalWallAreaSqft,
                    plasterAreaSqft + other.plasterAreaSqft, roomsAreaSqft + other.roomsAreaSqft);
        }
    }

    private PlanGeometry() {
    }

    static Derived derive(List<RoomDto> rooms, List<OpeningDto> openings) {
        if (rooms == null || rooms.isEmpty()) {
            return Derived.NONE;
        }

        // Count how many rooms use each half-foot piece of wall: once = external, twice = shared.
        Map<String, Integer> edges = new HashMap<>();
        double perimeterSum = 0;
        double roomsArea = 0;
        for (RoomDto room : rooms) {
            int x0 = units(room.x().doubleValue());
            int y0 = units(room.y().doubleValue());
            int x1 = x0 + units(room.widthFt().doubleValue());
            int y1 = y0 + units(room.depthFt().doubleValue());
            for (int x = x0; x < x1; x++) {
                edges.merge("H:" + y0 + ":" + x, 1, Integer::sum);
                edges.merge("H:" + y1 + ":" + x, 1, Integer::sum);
            }
            for (int y = y0; y < y1; y++) {
                edges.merge("V:" + x0 + ":" + y, 1, Integer::sum);
                edges.merge("V:" + x1 + ":" + y, 1, Integer::sum);
            }
            perimeterSum += 2 * (room.widthFt().doubleValue() + room.depthFt().doubleValue());
            roomsArea += room.widthFt().doubleValue() * room.depthFt().doubleValue();
        }

        double externalFt = 0;
        double internalFt = 0;
        for (int count : edges.values()) {
            if (count == 1) externalFt += UNIT_FT;
            else internalFt += UNIT_FT;
        }

        double externalOpenings = 0;
        double internalOpenings = 0;
        if (openings != null) {
            Map<String, RoomDto> byId = new HashMap<>();
            for (RoomDto room : rooms) byId.put(room.id(), room);
            for (OpeningDto opening : openings) {
                RoomDto room = byId.get(opening.roomId());
                if (room == null) continue;
                double area = opening.widthFt().doubleValue()
                        * (opening.kind() == OpeningKind.DOOR ? DOOR_HEIGHT_FT : WINDOW_HEIGHT_FT);
                if (isExternal(edges, room, opening)) externalOpenings += area;
                else internalOpenings += area;
            }
        }

        double externalGross = externalFt * FLOOR_HEIGHT_FT;
        double internalGross = internalFt * FLOOR_HEIGHT_FT;
        double externalNet = Math.max(externalGross - externalOpenings, 0);
        double internalNet = Math.max(internalGross - internalOpenings, 0);

        // Inside faces of every room, plus the outside face of the external walls. An opening on a
        // shared wall is cut out of both rooms' faces, one on an external wall out of one inside
        // face and the outside face (counted below).
        double insideFaces = perimeterSum * FLOOR_HEIGHT_FT - 2 * internalOpenings - externalOpenings;
        double plaster = Math.max(insideFaces, 0) + externalNet;

        return new Derived(externalFt, internalFt, externalNet, internalNet, plaster, roomsArea);
    }

    private static boolean isExternal(Map<String, Integer> edges, RoomDto room, OpeningDto opening) {
        int x0 = units(room.x().doubleValue());
        int y0 = units(room.y().doubleValue());
        int x1 = x0 + units(room.widthFt().doubleValue());
        int y1 = y0 + units(room.depthFt().doubleValue());
        int mid = (int) Math.floor((opening.offsetFt().doubleValue() + opening.widthFt().doubleValue() / 2) / UNIT_FT);
        String key = switch (opening.side()) {
            case NORTH -> "H:" + y0 + ":" + (x0 + mid);
            case SOUTH -> "H:" + y1 + ":" + (x0 + mid);
            case WEST -> "V:" + x0 + ":" + (y0 + mid);
            case EAST -> "V:" + x1 + ":" + (y0 + mid);
        };
        return edges.getOrDefault(key, 0) == 1;
    }

    private static int units(double feet) {
        return (int) Math.round(feet / UNIT_FT);
    }
}
