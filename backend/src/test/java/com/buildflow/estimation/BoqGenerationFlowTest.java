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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the full pre-construction planning flow: land + room checklist -> preliminary BOQ,
 * using the same 30x50 plot / 1,800 sqft G+1 / STANDARD grade example worked through by hand
 * when the rule table was designed. Expected quantities below are that hand calculation.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BoqGenerationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void generatesPreliminaryBoqFromRoomChecklistForAnEighteenHundredSqftGPlus1House() throws Exception {
        String token = registerAndGetToken("Madurai Home Builders", "owner-estimation-a@buildflow.test");

        long houseRequirementId = createHouseRequirement(token);

        // Must fail loudly rather than price a line at zero when the rate master isn't seeded yet.
        mockMvc.perform(post("/api/house-requirements/" + houseRequirementId + "/generate-boq")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        seedRateMaster(token);

        MvcResult generateResult = mockMvc.perform(post("/api/house-requirements/" + houseRequirementId + "/generate-boq")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBuiltupAreaSqft").value(1800.0))
                .andReturn();

        JsonNode data = objectMapper.readTree(generateResult.getResponse().getContentAsString()).path("data");
        long projectId = data.path("projectId").asLong();

        Map<String, JsonNode> byRuleCode = new HashMap<>();
        data.path("items").forEach(item -> byRuleCode.put(item.path("sourceRuleCode").asText(), item));

        // 9 core rules plus plaster cement and plaster sand, which reuse the already-priced items.
        assertEquals(11, byRuleCode.size());
        assertQuantity(byRuleCode, "CEMENT_PLASTER_STANDARD", "260");
        assertQuantity(byRuleCode, "SAND_PLASTER_STANDARD", "22.68");
        assertQuantity(byRuleCode, "CEMENT_RCC_STANDARD", "742");
        assertQuantity(byRuleCode, "CEMENT_FOUNDATION_STANDARD", "87");
        assertQuantity(byRuleCode, "STEEL_RCC_STANDARD", "6489.00");
        assertQuantity(byRuleCode, "BRICK_MASONRY_STANDARD", "17955");
        assertQuantity(byRuleCode, "SAND_MASONRY_STANDARD", "66.15");
        assertQuantity(byRuleCode, "AGGREGATE_RCC_STANDARD", "51.91");
        assertQuantity(byRuleCode, "TILE_FLOORING_STANDARD", "2041.20");
        assertQuantity(byRuleCode, "PAINT_EMULSION_STANDARD", "226.80");
        assertQuantity(byRuleCode, "DOOR_FLUSH_STANDARD", "10");

        assertEquals("SYSTEM_PRELIMINARY", byRuleCode.get("CEMENT_RCC_STANDARD").path("estimateSource").asText());
        assertTrue(byRuleCode.get("CEMENT_RCC_STANDARD").path("quantityLow").asDouble() > 0,
                "System-generated lines should carry a low/high band, not just a point estimate.");

        // A manually-added line (e.g. labour, which the seeded rules don't cover) on the same project.
        mockMvc.perform(post("/api/projects/" + projectId + "/boq/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"itemName": "Mason Labour", "category": "LABOUR", "unit": "Day", "quantity": 120, "rate": 900}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.estimateSource").value("MANUAL"));

        // Regenerating must replace only the system-generated lines, leaving the manual one intact.
        mockMvc.perform(post("/api/house-requirements/" + houseRequirementId + "/generate-boq")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        MvcResult listResult = mockMvc.perform(get("/api/projects/" + projectId + "/boq")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = objectMapper.readTree(listResult.getResponse().getContentAsString()).path("data");
        assertEquals(12, items.size(), "11 system-generated lines plus the 1 manual line survive a regenerate.");
        long manualCount = 0;
        for (JsonNode item : items) {
            if ("Mason Labour".equals(item.path("itemName").asText())) {
                manualCount++;
                assertEquals("MANUAL", item.path("estimateSource").asText());
            }
        }
        assertEquals(1, manualCount);

        mockMvc.perform(get("/api/house-requirements/" + houseRequirementId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ESTIMATED"))
                .andExpect(jsonPath("$.data.floors.length()").value(2));
    }

    @Test
    void itemsWithoutARateAreSkippedAndReportedAndPriceOnceARateIsAdded() throws Exception {
        String token = registerAndGetToken("Labour Estimates Co", "owner-estimation-e@buildflow.test");
        long houseRequirementId = createHouseRequirement(token);
        seedRateMaster(token);

        JsonNode first = generateData(token, houseRequirementId);
        java.util.List<String> unpriced = new java.util.ArrayList<>();
        first.path("unpricedItems").forEach(n -> unpriced.add(n.asText()));
        assertTrue(unpriced.contains("Mason Labour"), "Labour without a wage rate is reported, not priced at zero.");
        assertTrue(unpriced.contains("Copper Wire"));
        assertTrue(unpriced.contains("Waterproofing Chemical"));
        assertTrue(!unpriced.contains("OPC 53 Grade Cement"));

        mockMvc.perform(post("/api/rate-master")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"itemName": "Mason Labour", "category": "LABOUR", "unit": "day", "standardRate": 900}
                                """))
                .andExpect(status().isCreated());

        JsonNode second = generateData(token, houseRequirementId);
        Map<String, JsonNode> byRuleCode = new HashMap<>();
        second.path("items").forEach(item -> byRuleCode.put(item.path("sourceRuleCode").asText(), item));
        // 1,800 sqft * 0.20 days/sqft, whole days.
        assertQuantity(byRuleCode, "LABOUR_MASON_STANDARD", "360");
        assertEquals("LABOUR", byRuleCode.get("LABOUR_MASON_STANDARD").path("category").asText());
        assertTrue(!second.path("unpricedItems").toString().contains("Mason Labour"));
    }

    private JsonNode generateData(String token, long requirementId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/house-requirements/" + requirementId + "/generate-boq")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    @Test
    void editingRequirementResetsStatusAndRegenerationReflectsTheNewFloorArea() throws Exception {
        String token = registerAndGetToken("Coimbatore Home Builders", "owner-estimation-d@buildflow.test");

        long houseRequirementId = createHouseRequirement(token);
        seedRateMaster(token);

        mockMvc.perform(post("/api/house-requirements/" + houseRequirementId + "/generate-boq")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Widen the ground floor from 1,050 to 1,200 sqft; total built-up goes from 1,800 to 1,950.
        String updatedPayload = """
                {
                  "label": "30x50 plot - Madurai (revised)",
                  "location": "Madurai, Tamil Nadu",
                  "plotWidthFt": 30,
                  "plotLengthFt": 50,
                  "constructionGrade": "STANDARD",
                  "floors": [
                    {"floorLevel": 0, "floorAreaSqft": 1200, "bedroomCount": 2, "bathroomCount": 1,
                     "hasKitchen": true, "hasHall": true, "doorCount": 6, "windowCount": 8},
                    {"floorLevel": 1, "floorAreaSqft": 750, "bedroomCount": 1, "bathroomCount": 1,
                     "hasBalcony": true, "doorCount": 4, "windowCount": 6}
                  ]
                }
                """;

        mockMvc.perform(put("/api/house-requirements/" + houseRequirementId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(updatedPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.floors.length()").value(2))
                .andExpect(jsonPath("$.data.floors[0].floorAreaSqft").value(1200.0));

        MvcResult regenerateResult = mockMvc.perform(post("/api/house-requirements/" + houseRequirementId + "/generate-boq")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBuiltupAreaSqft").value(1950.0))
                .andReturn();

        JsonNode data = objectMapper.readTree(regenerateResult.getResponse().getContentAsString()).path("data");
        Map<String, JsonNode> byRuleCode = new HashMap<>();
        data.path("items").forEach(item -> byRuleCode.put(item.path("sourceRuleCode").asText(), item));

        // 1,950 * 0.40 * 1.03 = 803.4, rounded up to whole bags.
        assertQuantity(byRuleCode, "CEMENT_RCC_STANDARD", "804");
    }

    @Test
    void houseRequirementsAreIsolatedByBusiness() throws Exception {
        String tokenA = registerAndGetToken("Coastal Constructions", "owner-estimation-b@buildflow.test");
        String tokenB = registerAndGetToken("Hilltop Builders", "owner-estimation-c@buildflow.test");

        long houseRequirementId = createHouseRequirement(tokenA);

        mockMvc.perform(get("/api/house-requirements/" + houseRequirementId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/house-requirements/" + houseRequirementId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"label": "Hijacked", "plotWidthFt": 10, "plotLengthFt": 10, "constructionGrade": "STANDARD",
                                 "floors": [{"floorLevel": 0, "floorAreaSqft": 500}]}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/house-requirements/" + houseRequirementId + "/generate-boq")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    private void assertQuantity(Map<String, JsonNode> byRuleCode, String ruleCode, String expectedQuantity) {
        JsonNode item = byRuleCode.get(ruleCode);
        assertNotNull(item, "Expected a generated line for rule " + ruleCode);
        BigDecimal actual = new BigDecimal(item.path("quantity").asText());
        assertEquals(0, actual.compareTo(new BigDecimal(expectedQuantity)),
                () -> "Rule " + ruleCode + " expected quantity " + expectedQuantity + " but was " + actual);
    }

    private long createHouseRequirement(String token) throws Exception {
        String payload = """
                {
                  "label": "30x50 plot - Madurai",
                  "location": "Madurai, Tamil Nadu",
                  "plotWidthFt": 30,
                  "plotLengthFt": 50,
                  "constructionGrade": "STANDARD",
                  "floors": [
                    {"floorLevel": 0, "floorAreaSqft": 1050, "bedroomCount": 2, "bathroomCount": 1,
                     "hasKitchen": true, "hasHall": true, "doorCount": 6, "windowCount": 8},
                    {"floorLevel": 1, "floorAreaSqft": 750, "bedroomCount": 1, "bathroomCount": 1,
                     "hasBalcony": true, "doorCount": 4, "windowCount": 6}
                  ]
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/house-requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private void seedRateMaster(String token) throws Exception {
        record Rate(String itemName, String unit, String rate) {
        }
        List<Rate> rates = List.of(
                new Rate("OPC 53 Grade Cement", "bag", "420"),
                new Rate("TMT Steel Fe500", "kg", "65"),
                new Rate("Red Clay Brick 9x4x3", "nos", "9"),
                new Rate("River Sand", "cum", "1800"),
                new Rate("Aggregate 20mm", "cum", "1600"),
                new Rate("Vitrified Tile", "sqft", "65"),
                new Rate("Emulsion Paint", "ltr", "220"),
                new Rate("Flush Door", "nos", "3500")
        );

        for (Rate rate : rates) {
            mockMvc.perform(post("/api/rate-master")
                            .header("Authorization", "Bearer " + token)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {"itemName": "%s", "category": "MATERIAL", "unit": "%s", "standardRate": %s}
                                    """.formatted(rate.itemName(), rate.unit(), rate.rate())))
                    .andExpect(status().isCreated());
        }
    }

    private String registerAndGetToken(String businessName, String email) throws Exception {
        RegisterRequest request = new RegisterRequest(businessName, "Owner", email, "SecurePass123");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }
}
