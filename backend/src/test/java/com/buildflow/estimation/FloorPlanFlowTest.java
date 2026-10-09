package com.buildflow.estimation;

import com.buildflow.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FloorPlanFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void suggestedLayoutsFitThePlotDoNotOverlapAndMatchTheChecklist() throws Exception {
        String token = registerAndGetToken("Plan Builders", "owner-plan-a@buildflow.test");
        long id = createRequirement(token);

        MvcResult result = mockMvc.perform(post("/api/house-requirements/" + id + "/plans/suggest")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.floors.length()").value(2))
                .andReturn();
        JsonNode plans = data(result);
        double plotWidth = plans.path("plotWidthFt").asDouble();
        double plotDepth = plans.path("plotLengthFt").asDouble();
        assertEquals(30.0, plotWidth);
        assertEquals(50.0, plotDepth);

        Map<String, String> staircaseGeometry = new HashMap<>();
        for (JsonNode floor : plans.path("floors")) {
            assertFalse(floor.path("saved").asBoolean(), "Suggestions are not saved.");
            JsonNode rooms = floor.path("rooms");
            assertTrue(rooms.size() > 0);

            for (int i = 0; i < rooms.size(); i++) {
                JsonNode a = rooms.get(i);
                double ax = a.path("x").asDouble();
                double ay = a.path("y").asDouble();
                double aw = a.path("widthFt").asDouble();
                double ad = a.path("depthFt").asDouble();
                assertTrue(aw >= 2.5 && ad >= 2.5, a.path("name").asText() + " is too small: " + aw + " x " + ad);
                assertTrue(ax >= 0 && ay >= 0 && ax + aw <= plotWidth + 0.01 && ay + ad <= plotDepth + 0.01,
                        a.path("name").asText() + " is outside the plot");
                for (int j = i + 1; j < rooms.size(); j++) {
                    JsonNode b = rooms.get(j);
                    double overlapX = Math.min(ax + aw, b.path("x").asDouble() + b.path("widthFt").asDouble()) - Math.max(ax, b.path("x").asDouble());
                    double overlapY = Math.min(ay + ad, b.path("y").asDouble() + b.path("depthFt").asDouble()) - Math.max(ay, b.path("y").asDouble());
                    assertFalse(overlapX > 0.05 && overlapY > 0.05,
                            a.path("name").asText() + " overlaps " + b.path("name").asText());
                }
                if ("STAIRCASE".equals(a.path("type").asText())) {
                    staircaseGeometry.merge(floor.path("floorLevel").asText(),
                            ax + "," + ay + "," + aw + "," + ad, (x, y) -> x);
                }
            }

            // Every opening refers to a room on the same floor.
            for (JsonNode opening : floor.path("openings")) {
                boolean found = false;
                for (JsonNode room : rooms) {
                    if (room.path("id").asText().equals(opening.path("roomId").asText())) found = true;
                }
                assertTrue(found, "Opening points at a missing room");
            }
        }

        JsonNode ground = plans.path("floors").get(0);
        JsonNode first = plans.path("floors").get(1);
        assertEquals(2, ground.path("metrics").path("bedrooms").asInt());
        assertEquals(1, ground.path("metrics").path("bathrooms").asInt());
        assertTrue(hasType(ground, "KITCHEN") && hasType(ground, "HALL") && hasType(ground, "POOJA"));
        assertEquals(1, first.path("metrics").path("bedrooms").asInt());
        assertTrue(hasType(first, "BALCONY") && !hasType(first, "KITCHEN"));

        // The staircase sits in the same place on every floor of a multi-storey house.
        assertEquals(2, staircaseGeometry.size());
        assertEquals(staircaseGeometry.get("0"), staircaseGeometry.get("1"));

        // The envelope tracks the requested floor area (1,050 and 750 sq.ft) closely.
        assertEquals(1050.0, ground.path("metrics").path("builtUpAreaSqft").asDouble(), 1050 * 0.12);
        assertEquals(750.0, first.path("metrics").path("builtUpAreaSqft").asDouble(), 750 * 0.12);
    }

    @Test
    void savedPlansAreValidatedAndApplyingThemUpdatesTheChecklist() throws Exception {
        String token = registerAndGetToken("Plan Savers", "owner-plan-b@buildflow.test");
        long id = createRequirement(token);

        String layout = """
                {"rooms": [
                  {"id": "a", "type": "HALL", "name": "Hall", "x": 3, "y": 5, "widthFt": 12, "depthFt": 14},
                  {"id": "b", "type": "BEDROOM", "name": "Bedroom", "x": 15, "y": 5, "widthFt": 12, "depthFt": 14},
                  {"id": "c", "type": "BEDROOM", "name": "Bedroom 2", "x": 3, "y": 19, "widthFt": 12, "depthFt": 12},
                  {"id": "d", "type": "BATHROOM", "name": "Bath", "x": 15, "y": 19, "widthFt": 6, "depthFt": 8},
                  {"id": "e", "type": "KITCHEN", "name": "Kitchen", "x": 21, "y": 19, "widthFt": 6, "depthFt": 12}],
                 "openings": [
                  {"id": "o1", "roomId": "a", "kind": "DOOR", "side": "NORTH", "offsetFt": 4, "widthFt": 3.5},
                  {"id": "o2", "roomId": "a", "kind": "WINDOW", "side": "WEST", "offsetFt": 5, "widthFt": 4},
                  {"id": "o3", "roomId": "b", "kind": "WINDOW", "side": "EAST", "offsetFt": 5, "widthFt": 4}]}
                """;
        mockMvc.perform(put("/api/house-requirements/" + id + "/plans/0")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(layout))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saved").value(true))
                // Built-up is the sum of the rooms: 168 + 168 + 144 + 48 + 72 = 600 sq.ft.
                .andExpect(jsonPath("$.data.metrics.builtUpAreaSqft").value(600.0))
                .andExpect(jsonPath("$.data.metrics.roomsAreaSqft").value(600.0))
                .andExpect(jsonPath("$.data.metrics.bedrooms").value(2))
                .andExpect(jsonPath("$.data.metrics.doors").value(1))
                .andExpect(jsonPath("$.data.metrics.windows").value(2));

        mockMvc.perform(get("/api/house-requirements/" + id + "/plans").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.floors[0].saved").value(true))
                .andExpect(jsonPath("$.data.floors[1].saved").value(false))
                .andExpect(jsonPath("$.data.floors[0].rooms.length()").value(5));

        // Replacing the plan replaces its rooms rather than adding to them.
        mockMvc.perform(put("/api/house-requirements/" + id + "/plans/0")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"rooms\": [{\"id\": \"a\", \"type\": \"HALL\", \"x\": 3, \"y\": 5, \"widthFt\": 12, \"depthFt\": 14}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rooms.length()").value(1));
        mockMvc.perform(put("/api/house-requirements/" + id + "/plans/0")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(layout))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/house-requirements/" + id + "/plans/apply").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.floors[0].floorAreaSqft").value(600.0))
                .andExpect(jsonPath("$.data.floors[0].bedroomCount").value(2))
                .andExpect(jsonPath("$.data.floors[0].bathroomCount").value(1))
                .andExpect(jsonPath("$.data.floors[0].hasKitchen").value(true))
                .andExpect(jsonPath("$.data.floors[0].hasHall").value(true))
                .andExpect(jsonPath("$.data.floors[0].hasBalcony").value(false))
                .andExpect(jsonPath("$.data.floors[0].doorCount").value(1))
                .andExpect(jsonPath("$.data.floors[0].windowCount").value(2))
                // The undrawn first floor keeps its checklist values.
                .andExpect(jsonPath("$.data.floors[1].floorAreaSqft").value(750.0));
    }

    @Test
    void invalidLayoutsAreRejectedAndPlansAreTenantScoped() throws Exception {
        String token = registerAndGetToken("Plan Guards", "owner-plan-c@buildflow.test");
        String other = registerAndGetToken("Plan Others", "owner-plan-d@buildflow.test");
        long id = createRequirement(token);

        String overlapping = """
                {"rooms": [
                  {"id": "a", "type": "HALL", "name": "Hall", "x": 3, "y": 5, "widthFt": 12, "depthFt": 12},
                  {"id": "b", "type": "BEDROOM", "name": "Bedroom", "x": 10, "y": 8, "widthFt": 10, "depthFt": 10}]}
                """;
        expectBadRequest(token, id, 0, overlapping);
        expectBadRequest(token, id, 0, "{\"rooms\": [{\"id\": \"a\", \"type\": \"HALL\", \"x\": 20, \"y\": 5, \"widthFt\": 12, \"depthFt\": 10}]}");
        expectBadRequest(token, id, 0, "{\"rooms\": [{\"id\": \"a\", \"type\": \"HALL\", \"x\": 3, \"y\": 5, \"widthFt\": 2, \"depthFt\": 10}]}");
        expectBadRequest(token, id, 0, """
                {"rooms": [{"id": "a", "type": "HALL", "x": 3, "y": 5, "widthFt": 10, "depthFt": 10}],
                 "openings": [{"id": "o1", "roomId": "zzz", "kind": "DOOR", "side": "NORTH", "offsetFt": 1, "widthFt": 3}]}
                """);
        expectBadRequest(token, id, 0, """
                {"rooms": [{"id": "a", "type": "HALL", "x": 3, "y": 5, "widthFt": 10, "depthFt": 10}],
                 "openings": [{"id": "o1", "roomId": "a", "kind": "DOOR", "side": "NORTH", "offsetFt": 8, "widthFt": 3}]}
                """);
        expectBadRequest(token, id, 0, """
                {"rooms": [{"id": "a", "type": "HALL", "x": 3, "y": 5, "widthFt": 10, "depthFt": 10},
                           {"id": "a", "type": "BEDROOM", "x": 14, "y": 5, "widthFt": 10, "depthFt": 10}]}
                """);
        // A floor that is not in the checklist cannot carry a plan.
        expectBadRequest(token, id, 7, "{\"rooms\": []}");

        mockMvc.perform(post("/api/house-requirements/" + id + "/plans/apply").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/house-requirements/" + id + "/plans").header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/house-requirements/" + id + "/plans/suggest").header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/house-requirements/" + id + "/plans/0")
                        .header("Authorization", "Bearer " + other)
                        .contentType(APPLICATION_JSON)
                        .content("{\"rooms\": []}"))
                .andExpect(status().isNotFound());
    }


    @Test
    void drawnPlanReplacesAreaRulesForWallsPlasterPaintAndFlooring() throws Exception {
        String token = registerAndGetToken("Plan Quantities", "owner-plan-e@buildflow.test");
        String[][] rates = {{"OPC 53 Grade Cement", "bag", "420"}, {"TMT Steel Fe500", "kg", "65"},
                {"Red Clay Brick 9x4x3", "nos", "9"}, {"River Sand", "cum", "1800"},
                {"Aggregate 20mm", "cum", "1600"}, {"Vitrified Tile", "sqft", "65"},
                {"Emulsion Paint", "ltr", "220"}, {"Flush Door", "nos", "3500"}};
        for (String[] rate : rates) {
            mockMvc.perform(post("/api/rate-master")
                            .header("Authorization", "Bearer " + token)
                            .contentType(APPLICATION_JSON)
                            .content("{\"itemName\": \"%s\", \"category\": \"MATERIAL\", \"unit\": \"%s\", \"standardRate\": %s}"
                                    .formatted(rate[0], rate[1], rate[2])))
                    .andExpect(status().isCreated());
        }
        MvcResult created = mockMvc.perform(post("/api/house-requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"label": "Two rooms", "plotWidthFt": 30, "plotLengthFt": 50, "constructionGrade": "STANDARD",
                                 "floors": [{"floorLevel": 0, "floorAreaSqft": 200, "bedroomCount": 1, "hasHall": true, "doorCount": 2}]}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long id = data(created).path("id").asLong();

        // Without a drawing the thumb rules apply.
        MvcResult before = mockMvc.perform(post("/api/house-requirements/" + id + "/generate-boq")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantityBasis").value("AREA_RULES"))
                .andReturn();
        assertTrue(hasLine(data(before), "BRICK_MASONRY_STANDARD"));
        assertFalse(hasLine(data(before), "BRICK_EXT_DRAWN_STANDARD"));

        // Two 10 x 10 ft rooms side by side: 60 ft of external wall, 10 ft shared. Floor height 10 ft.
        // Openings: external door 3x7 and window 4x4 (37 sq.ft), plus an internal door 3x7 (21 sq.ft).
        mockMvc.perform(put("/api/house-requirements/" + id + "/plans/0")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"rooms": [
                                  {"id": "a", "type": "HALL", "name": "Hall", "x": 0, "y": 0, "widthFt": 10, "depthFt": 10},
                                  {"id": "b", "type": "BEDROOM", "name": "Bedroom", "x": 10, "y": 0, "widthFt": 10, "depthFt": 10}],
                                 "openings": [
                                  {"id": "o1", "roomId": "a", "kind": "DOOR", "side": "NORTH", "offsetFt": 3.5, "widthFt": 3},
                                  {"id": "o2", "roomId": "b", "kind": "WINDOW", "side": "SOUTH", "offsetFt": 3, "widthFt": 4},
                                  {"id": "o3", "roomId": "a", "kind": "DOOR", "side": "EAST", "offsetFt": 3.5, "widthFt": 3}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metrics.externalWallFt").value(60.0))
                .andExpect(jsonPath("$.data.metrics.internalWallFt").value(10.0))
                // External 600 - 37 = 563; internal 100 - 21 = 79.
                .andExpect(jsonPath("$.data.metrics.wallAreaSqft").value(642.0))
                // Inside faces 800 - 2 x 21 - 37 = 721, plus the 563 outside face.
                .andExpect(jsonPath("$.data.metrics.plasterAreaSqft").value(1284.0));

        MvcResult after = mockMvc.perform(post("/api/house-requirements/" + id + "/generate-boq")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantityBasis").value("DRAWN_PLAN"))
                .andReturn();
        JsonNode generated = data(after);
        assertFalse(hasLine(generated, "BRICK_MASONRY_STANDARD"), "Thumb rules are replaced by drawn rules.");
        assertFalse(hasLine(generated, "TILE_FLOORING_STANDARD"));

        // 563 x 9.3 x 1.05 = 5497.7 and 79 x 4.65 x 1.05 = 385.7, rounded up to whole bricks.
        assertQuantity(generated, "BRICK_EXT_DRAWN_STANDARD", "5498");
        assertQuantity(generated, "BRICK_INT_DRAWN_STANDARD", "386");
        // 1,284 sq.ft of plaster: cement 0.0082 x 1.03 -> 11 bags; sand 0.0012 x 1.05 -> 1.62 cum; paint 0.0167 x 1.05 -> 22.51 l.
        assertQuantity(generated, "CEMENT_PLASTER_DRAWN_STANDARD", "11");
        assertQuantity(generated, "SAND_PLASTER_DRAWN_STANDARD", "1.62");
        assertQuantity(generated, "PAINT_DRAWN_STANDARD", "22.51");
        // 200 sq.ft of rooms x 1.08 wastage.
        assertQuantity(generated, "TILE_DRAWN_STANDARD", "216");
    }


    @Test
    void drawnColumnsGiveBeamsAndFrameQuantitiesThatReplaceTheConcreteAreaRules() throws Exception {
        String token = registerAndGetToken("Frame Quantities", "owner-plan-f@buildflow.test");
        mockMvc.perform(post("/api/rate-master/starter").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        MvcResult created = mockMvc.perform(post("/api/house-requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"label": "Two rooms", "plotWidthFt": 30, "plotLengthFt": 50, "constructionGrade": "STANDARD",
                                 "floors": [{"floorLevel": 0, "floorAreaSqft": 200, "bedroomCount": 1, "hasHall": true, "doorCount": 2}]}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long id = data(created).path("id").asLong();

        MvcResult suggested = mockMvc.perform(post("/api/house-requirements/" + id + "/plans/suggest")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(data(suggested).path("floors").get(0).path("columns").size() >= 4);

        String rooms = """
                "rooms": [
                  {"id": "a", "type": "HALL", "name": "Hall", "x": 0, "y": 0, "widthFt": 10, "depthFt": 10},
                  {"id": "b", "type": "BEDROOM", "name": "Bedroom", "x": 10, "y": 0, "widthFt": 10, "depthFt": 10}]
                """;
        String cols = """
                "columns": [
                  {"id": "c1", "x": 0, "y": 0, "widthFt": 0.75, "depthFt": 1},
                  {"id": "c2", "x": 10, "y": 0, "widthFt": 0.75, "depthFt": 1},
                  {"id": "c3", "x": 20, "y": 0, "widthFt": 0.75, "depthFt": 1},
                  {"id": "c4", "x": 0, "y": 10, "widthFt": 0.75, "depthFt": 1},
                  {"id": "c5", "x": 10, "y": 10, "widthFt": 0.75, "depthFt": 1},
                  {"id": "c6", "x": 20, "y": 10, "widthFt": 0.75, "depthFt": 1}]
                """;

        // Beams run only along walls: 20 + 20 along the top and bottom, 10 each on three vertical walls = 70 ft.
        // Concrete: columns 6 x .75 x 1 x 10 = 45 ft3, beams 70 x 2 levels x .75 = 105 ft3, slab 200 x 5/12 = 83.33 ft3,
        // footings 6 x 4 x 4 x 1.25 = 120 ft3: 353.33 ft3 = 10.005 m3. Steel 0.8/1.5/1.5/0.8 % at 7850 kg/m3.
        mockMvc.perform(put("/api/house-requirements/" + id + "/plans/0")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{" + rooms + "," + cols + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.columns.length()").value(6))
                .andExpect(jsonPath("$.data.beams.length()").value(7))
                .andExpect(jsonPath("$.data.structure.columnCount").value(6))
                .andExpect(jsonPath("$.data.structure.beamLengthFt").value(70.0))
                .andExpect(jsonPath("$.data.structure.concreteM3").value(org.hamcrest.Matchers.closeTo(10.005, 0.02)))
                .andExpect(jsonPath("$.data.structure.steelKg").value(org.hamcrest.Matchers.closeTo(861.7, 2.0)));

        MvcResult generated = mockMvc.perform(post("/api/house-requirements/" + id + "/generate-boq")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.structuralBasis").value("DRAWN_STRUCTURE"))
                .andReturn();
        JsonNode result = data(generated);
        assertFalse(hasLine(result, "CEMENT_RCC_STANDARD"), "Area-based RCC rules are replaced.");
        assertFalse(hasLine(result, "STEEL_RCC_STANDARD"));
        assertFalse(hasLine(result, "CEMENT_FOUNDATION_STANDARD"));
        // 10.005 m3 x 8.07 x 1.03 = 83.2 bags (rounded up); sand 10.005 x .42 x 1.05 = 4.41; aggregate 10.005 x .84 x 1.03 = 8.66.
        assertApprox(result, "CEMENT_STRUCT_STANDARD", 84, 1.5);
        assertApprox(result, "SAND_STRUCT_STANDARD", 4.41, 0.1);
        assertApprox(result, "AGGREGATE_STRUCT_STANDARD", 8.66, 0.15);
        assertApprox(result, "STEEL_STRUCT_STANDARD", 887.5, 3);

        // Fewer than four columns is not a frame, so the area rules stay in force.
        mockMvc.perform(put("/api/house-requirements/" + id + "/plans/0")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{" + rooms + ", \"columns\": [{\"id\": \"c1\", \"x\": 0, \"y\": 0, \"widthFt\": 0.75, \"depthFt\": 1}]}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/house-requirements/" + id + "/generate-boq").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.structuralBasis").value("AREA_RULES"));

        mockMvc.perform(put("/api/house-requirements/" + id + "/plans/0")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{" + rooms + ", \"columns\": [{\"id\": \"c1\", \"x\": 99, \"y\": 0, \"widthFt\": 0.75, \"depthFt\": 1}]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/house-requirements/" + id + "/plans/0")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{" + rooms + ", \"columns\": [{\"id\": \"c1\", \"x\": 0, \"y\": 0, \"widthFt\": 0.75, \"depthFt\": 1}, {\"id\": \"c1\", \"x\": 5, \"y\": 0, \"widthFt\": 0.75, \"depthFt\": 1}]}"))
                .andExpect(status().isBadRequest());
    }

    private void assertApprox(JsonNode generated, String ruleCode, double expected, double tolerance) {
        for (JsonNode item : generated.path("items")) {
            if (ruleCode.equals(item.path("sourceRuleCode").asText())) {
                assertEquals(expected, item.path("quantity").asDouble(), tolerance, ruleCode);
                return;
            }
        }
        throw new AssertionError("No line for " + ruleCode);
    }


    @Test
    void furnitureIsSavedWithTheFloorValidatedAndExportedToCad() throws Exception {
        String token = registerAndGetToken("Furniture Flow", "owner-plan-furn@buildflow.test");
        long id = createRequirement(token);
        String rooms = "[{\"id\": \"a\", \"type\": \"BEDROOM\", \"name\": \"Bedroom\", \"x\": 5, \"y\": 5, \"widthFt\": 12, \"depthFt\": 12}]";
        String bed = "{\"id\": \"f1\", \"kind\": \"BED\", \"x\": 11, \"y\": 9, \"widthFt\": 5.5, \"depthFt\": 6.5, \"heightFt\": 1.6, \"rotationDeg\": 90, \"colour\": \"#7a5230\"}";

        MvcResult saved = mockMvc.perform(put("/api/house-requirements/" + id + "/plans/0")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"rooms\": " + rooms + ", \"furniture\": [" + bed + "]}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode item = data(saved).path("furniture").get(0);
        assertEquals("BED", item.path("kind").asText());
        assertEquals(90, item.path("rotationDeg").asInt());
        assertEquals("#7a5230", item.path("colour").asText());

        // It comes back when the plans are read, and saving again without it removes it.
        JsonNode read = data(mockMvc.perform(get("/api/house-requirements/" + id + "/plans")
                .header("Authorization", "Bearer " + token)).andReturn());
        assertEquals(1, read.path("floors").get(0).path("furniture").size());
        String dxf = mockMvc.perform(get("/api/house-requirements/" + id + "/plans/dxf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(dxf.contains("FURNITURE"));

        for (String bad : new String[]{
                bed.replace("BED", "SPACESHIP"),
                bed.replace("\"rotationDeg\": 90", "\"rotationDeg\": 45"),
                bed.replace("\"heightFt\": 1.6", "\"heightFt\": 50"),
                bed.replace("#7a5230", "red"),
                bed.replace("\"x\": 11", "\"x\": 900")}) {
            mockMvc.perform(put("/api/house-requirements/" + id + "/plans/0")
                            .header("Authorization", "Bearer " + token)
                            .contentType(APPLICATION_JSON)
                            .content("{\"rooms\": " + rooms + ", \"furniture\": [" + bad + "]}"))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(put("/api/house-requirements/" + id + "/plans/0")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"rooms\": " + rooms + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.furniture.length()").value(0));
    }

    @Test
    void drawnPlansExportAsADxfFileAndNothingDrawnIsRefused() throws Exception {
        String token = registerAndGetToken("Dxf Export", "owner-plan-dxf@buildflow.test");
        long id = createRequirement(token);

        mockMvc.perform(get("/api/house-requirements/" + id + "/plans/dxf").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        MvcResult suggested = mockMvc.perform(post("/api/house-requirements/" + id + "/plans/suggest")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode floor : data(suggested).path("floors")) {
            mockMvc.perform(put("/api/house-requirements/" + id + "/plans/" + floor.path("floorLevel").asInt())
                            .header("Authorization", "Bearer " + token)
                            .contentType(APPLICATION_JSON)
                            .content("{\"rooms\": " + floor.path("rooms") + ", \"openings\": " + floor.path("openings")
                                    + ", \"columns\": " + floor.path("columns") + "}"))
                    .andExpect(status().isOk());
        }
        MvcResult result = mockMvc.perform(get("/api/house-requirements/" + id + "/plans/dxf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String dxf = result.getResponse().getContentAsString();
        assertTrue(result.getResponse().getHeader("Content-Disposition").contains(".dxf"));
        assertTrue(dxf.startsWith("0\nSECTION"));
        assertTrue(dxf.trim().endsWith("EOF"));
        assertTrue(dxf.contains("\nWALLS\n") && dxf.contains("\nCOLUMNS\n") && dxf.contains("\nBEAMS\n"));
        assertTrue(dxf.contains("GROUND FLOOR") && dxf.contains("Hall"));
    }

    @Test
    void gridColumnsAreFewerThanJunctionColumnsYetKeepSpansAndCornersSupported() throws Exception {
        String token = registerAndGetToken("Grid Columns", "owner-plan-g@buildflow.test");
        long id = createRequirement(token);

        MvcResult suggested = mockMvc.perform(post("/api/house-requirements/" + id + "/plans/suggest")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode floor : data(suggested).path("floors")) {
            JsonNode rooms = floor.path("rooms");
            JsonNode grid = floor.path("columns");
            String layout = "{\"rooms\": " + rooms.toString() + ", \"openings\": []}";
            MvcResult junctions = mockMvc.perform(post("/api/house-requirements/" + id + "/plans/columns/suggest")
                            .param("style", "junctions")
                            .header("Authorization", "Bearer " + token)
                            .contentType(APPLICATION_JSON)
                            .content(layout))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode everyJunction = data(junctions);
            assertTrue(grid.size() >= 4);
            assertTrue(grid.size() < everyJunction.size(),
                    "Grid " + grid.size() + " should be fewer than junction " + everyJunction.size());

            // Every column lies on a wall of some room (touches a room edge).
            double minX = 1e9, minY = 1e9, maxX = 0, maxY = 0;
            for (JsonNode r : rooms) {
                minX = Math.min(minX, r.path("x").asDouble());
                minY = Math.min(minY, r.path("y").asDouble());
                maxX = Math.max(maxX, r.path("x").asDouble() + r.path("widthFt").asDouble());
                maxY = Math.max(maxY, r.path("y").asDouble() + r.path("depthFt").asDouble());
            }
            // All four building corners carry a column.
            for (double[] corner : new double[][]{{minX, minY}, {maxX, minY}, {minX, maxY}, {maxX, maxY}}) {
                assertTrue(hasColumn(grid, corner[0], corner[1]), "Missing corner column at " + corner[0] + "," + corner[1]);
            }
            // No beam, which runs along a wall between columns, is longer than 15 ft.
            for (JsonNode beam : floor.path("beams")) {
                double length = Math.abs(beam.path("x2").asDouble() - beam.path("x1").asDouble())
                        + Math.abs(beam.path("y2").asDouble() - beam.path("y1").asDouble());
                assertTrue(length <= 15.01, "Beam too long: " + length + " ft");
            }
        }
    }

    private boolean hasColumn(JsonNode columns, double x, double y) {
        for (JsonNode c : columns) {
            if (Math.abs(c.path("x").asDouble() - x) < 0.01 && Math.abs(c.path("y").asDouble() - y) < 0.01) return true;
        }
        return false;
    }

    private boolean hasLine(JsonNode generated, String ruleCode) {
        for (JsonNode item : generated.path("items")) {
            if (ruleCode.equals(item.path("sourceRuleCode").asText())) return true;
        }
        return false;
    }

    private void assertQuantity(JsonNode generated, String ruleCode, String expected) {
        for (JsonNode item : generated.path("items")) {
            if (ruleCode.equals(item.path("sourceRuleCode").asText())) {
                assertEquals(0, new java.math.BigDecimal(expected).compareTo(new java.math.BigDecimal(item.path("quantity").asText())),
                        ruleCode + " expected " + expected + " but was " + item.path("quantity").asText());
                return;
            }
        }
        throw new AssertionError("No line for " + ruleCode);
    }

    private void expectBadRequest(String token, long id, int floor, String body) throws Exception {
        mockMvc.perform(put("/api/house-requirements/" + id + "/plans/" + floor)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private boolean hasType(JsonNode floor, String type) {
        for (JsonNode room : floor.path("rooms")) {
            if (type.equals(room.path("type").asText())) return true;
        }
        return false;
    }

    private long createRequirement(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/house-requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"label": "30x50", "plotWidthFt": 30, "plotLengthFt": 50, "constructionGrade": "STANDARD",
                                 "floors": [
                                   {"floorLevel": 0, "floorAreaSqft": 1050, "bedroomCount": 2, "bathroomCount": 1,
                                    "hasKitchen": true, "hasHall": true, "hasPoojaRoom": true, "doorCount": 6},
                                   {"floorLevel": 1, "floorAreaSqft": 750, "bedroomCount": 1, "bathroomCount": 1,
                                    "hasBalcony": true, "doorCount": 4}]}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return data(result).path("id").asLong();
    }

    private JsonNode data(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String registerAndGetToken(String businessName, String email) throws Exception {
        RegisterRequest request = new RegisterRequest(businessName, "Owner", email, "SecurePass123");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return data(result).path("accessToken").asText();
    }
}
