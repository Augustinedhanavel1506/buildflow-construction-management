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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BoqGenerationRuleFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String RULE_BODY = """
            {"coefficient": 0.5, "wastagePercent": 3, "minCoefficient": 0.45, "maxCoefficient": 0.55,
             "sourceType": "COMPLETED_PROJECT_AVERAGE", "sourceReference": "Firm average of 12 completed houses",
             "confidenceLevel": "MEDIUM", "verified": true, "sampleSize": 12}
            """;

    @Test
    void overridingAPlatformRuleChangesGenerationForThatBusinessOnlyAndCanBeReverted() throws Exception {
        String tokenA = registerAndGetToken("Rule Owners Co", "owner-rules-a@buildflow.test");
        String tokenB = registerAndGetToken("Other Builders", "owner-rules-b@buildflow.test");

        JsonNode rules = listRules(tokenA);
        long platformRuleId = findRule(rules, "CEMENT_RCC_STANDARD").path("id").asLong();
        assertEquals("PLATFORM", findRule(rules, "CEMENT_RCC_STANDARD").path("scope").asText());
        int ruleCount = rules.size();

        mockMvc.perform(put("/api/boq-generation-rules/" + platformRuleId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content(RULE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scope").value("BUSINESS"))
                .andExpect(jsonPath("$.data.baseRuleCode").value("CEMENT_RCC_STANDARD"))
                .andExpect(jsonPath("$.data.version").value(1));

        // The override replaces the platform row in this business's list rather than duplicating it.
        JsonNode afterOverride = listRules(tokenA);
        assertEquals(ruleCount, afterOverride.size());
        assertEquals("BUSINESS", findRule(afterOverride, "CEMENT_RCC_STANDARD").path("scope").asText());

        // Business B still sees the untouched platform rule.
        assertEquals("PLATFORM", findRule(listRules(tokenB), "CEMENT_RCC_STANDARD").path("scope").asText());

        long overrideId = findRule(afterOverride, "CEMENT_RCC_STANDARD").path("id").asLong();
        mockMvc.perform(put("/api/boq-generation-rules/" + overrideId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content(RULE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2));

        long requirementId = createHouseRequirement(tokenA);
        seedRateMaster(tokenA);
        JsonNode generated = generate(tokenA, requirementId);
        // 1,800 sqft * 0.50 * 1.03 = 927 bags (platform default would be 742).
        assertEquals(0, new BigDecimal("927").compareTo(quantityOf(generated, "CEMENT_RCC_STANDARD")
                .orElseThrow()));

        // Tile/paint/door lines have no band but steel/cement do, so the range is now a real range.
        BigDecimal low = new BigDecimal(generated.path("estimatedCostLow").asText());
        BigDecimal high = new BigDecimal(generated.path("estimatedCostHigh").asText());
        assertTrue(low.compareTo(high) < 0, "Cost range should not collapse to a single number.");
        assertTrue(low.compareTo(new BigDecimal(generated.path("estimatedCost").asText())) <= 0);

        mockMvc.perform(delete("/api/boq-generation-rules/" + overrideId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
        assertEquals("PLATFORM", findRule(listRules(tokenA), "CEMENT_RCC_STANDARD").path("scope").asText());

        // Platform rules themselves cannot be reverted/deleted by a tenant.
        mockMvc.perform(delete("/api/boq-generation-rules/" + platformRuleId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ruleEditsAreValidated() throws Exception {
        String token = registerAndGetToken("Validation Co", "owner-rules-c@buildflow.test");
        long ruleId = findRule(listRules(token), "STEEL_RCC_STANDARD").path("id").asLong();

        mockMvc.perform(put("/api/boq-generation-rules/" + ruleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"coefficient": 3.5, "wastagePercent": 3, "minCoefficient": 4, "maxCoefficient": 5,
                                 "sourceType": "GOVT_SOR", "sourceReference": "TN PWD SOR 2025-26", "confidenceLevel": "HIGH"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/boq-generation-rules/" + ruleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"coefficient": 3.5, "wastagePercent": 3, "sourceType": "PLACEHOLDER",
                                 "sourceReference": "guess", "confidenceLevel": "LOW", "verified": true}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/boq-generation-rules/" + ruleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"coefficient": 3.5, "wastagePercent": 3, "sourceType": "GOVT_SOR",
                                 "sourceReference": " ", "confidenceLevel": "HIGH"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    private JsonNode listRules(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/boq-generation-rules")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private JsonNode findRule(JsonNode rules, String baseRuleCode) {
        for (JsonNode rule : rules) {
            if (baseRuleCode.equals(rule.path("baseRuleCode").asText())) {
                return rule;
            }
        }
        assertNotNull(null, "Rule not found: " + baseRuleCode);
        return null;
    }

    private java.util.Optional<BigDecimal> quantityOf(JsonNode generated, String sourceRuleCode) {
        for (JsonNode item : generated.path("items")) {
            if (item.path("sourceRuleCode").asText().startsWith(sourceRuleCode)) {
                return java.util.Optional.of(new BigDecimal(item.path("quantity").asText()));
            }
        }
        return java.util.Optional.empty();
    }

    private JsonNode generate(String token, long requirementId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/house-requirements/" + requirementId + "/generate-boq")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private long createHouseRequirement(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/house-requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"label": "30x50", "plotWidthFt": 30, "plotLengthFt": 50, "constructionGrade": "STANDARD",
                                 "floors": [
                                   {"floorLevel": 0, "floorAreaSqft": 1050, "doorCount": 6},
                                   {"floorLevel": 1, "floorAreaSqft": 750, "doorCount": 4}]}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private void seedRateMaster(String token) throws Exception {
        record Rate(String itemName, String unit, String rate) {
        }
        for (Rate rate : List.of(
                new Rate("OPC 53 Grade Cement", "bag", "420"), new Rate("TMT Steel Fe500", "kg", "65"),
                new Rate("Red Clay Brick 9x4x3", "nos", "9"), new Rate("River Sand", "cum", "1800"),
                new Rate("Aggregate 20mm", "cum", "1600"), new Rate("Vitrified Tile", "sqft", "65"),
                new Rate("Emulsion Paint", "ltr", "220"), new Rate("Flush Door", "nos", "3500"))) {
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
