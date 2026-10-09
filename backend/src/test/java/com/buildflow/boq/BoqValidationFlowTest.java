package com.buildflow.boq;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BoqValidationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void engineerValidationSurvivesRegenerationAndEditingMakesALineManual() throws Exception {
        String token = registerAndGetToken("Validation Builders", "owner-validate-a@buildflow.test");
        long[] ids = createEstimate(token);
        long requirementId = ids[0];
        long projectId = ids[1];

        JsonNode items = boq(token, projectId);
        int originalCount = items.size();
        JsonNode cementRcc = bySourceRule(items, "CEMENT_RCC_STANDARD");
        long cementId = cementRcc.path("id").asLong();

        // The engineer corrects cement from 742 to 800 bags.
        mockMvc.perform(post("/api/boq/items/" + cementId + "/validate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"engineerName\": \"R. Kumar, B.E. (Civil)\", \"quantity\": 800, \"note\": \"Heavier slab per structural drawing\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estimateSource").value("ENGINEER_VALIDATED"))
                .andExpect(jsonPath("$.data.validatedBy").value("R. Kumar, B.E. (Civil)"))
                .andExpect(jsonPath("$.data.quantity").value(800))
                .andExpect(jsonPath("$.data.estimatedAmount").value(336000.0))
                .andExpect(jsonPath("$.data.quantityLow").isEmpty());
        // The project headline estimate follows the corrected line (800 bags instead of 742).
        MvcResult project = mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        BigDecimal projectEstimate = new BigDecimal(objectMapper.readTree(project.getResponse().getContentAsString())
                .path("data").path("estimatedCost").asText());
        BigDecimal sumOfLines = BigDecimal.ZERO;
        for (JsonNode line : boq(token, projectId)) {
            sumOfLines = sumOfLines.add(new BigDecimal(line.path("estimatedAmount").asText()));
        }
        assertEquals(0, sumOfLines.compareTo(projectEstimate));

        // Approve everything else as estimated.
        mockMvc.perform(post("/api/projects/" + projectId + "/boq/validate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"engineerName\": \"R. Kumar, B.E. (Civil)\"}"))
                .andExpect(status().isOk());
        // Nothing left to approve.
        mockMvc.perform(post("/api/projects/" + projectId + "/boq/validate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"engineerName\": \"R. Kumar\"}"))
                .andExpect(status().isBadRequest());

        // Regenerating must not create preliminary duplicates next to validated lines.
        mockMvc.perform(post("/api/house-requirements/" + requirementId + "/generate-boq")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        JsonNode afterRegenerate = boq(token, projectId);
        assertEquals(originalCount, afterRegenerate.size());
        assertEquals(0, new BigDecimal("800").compareTo(new BigDecimal(
                bySourceRule(afterRegenerate, "CEMENT_RCC_STANDARD").path("quantity").asText())));
        for (JsonNode item : afterRegenerate) {
            assertEquals("ENGINEER_VALIDATED", item.path("estimateSource").asText());
        }

        // Hand-editing a validated line removes the engineer's sign-off and makes it the user's own.
        mockMvc.perform(put("/api/boq/items/" + cementId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"itemName\": \"OPC 53 Grade Cement\", \"category\": \"MATERIAL\", \"unit\": \"bag\", \"quantity\": 900, \"rate\": 420}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estimateSource").value("MANUAL"))
                .andExpect(jsonPath("$.data.validatedBy").isEmpty());

        // A manual line cannot be "engineer-validated".
        mockMvc.perform(post("/api/boq/items/" + cementId + "/validate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"engineerName\": \"R. Kumar\"}"))
                .andExpect(status().isBadRequest());

        // Recording only actual spend on a validated line leaves its status alone.
        long steelId = bySourceRule(boq(token, projectId), "STEEL_RCC_STANDARD").path("id").asLong();
        JsonNode steel = bySourceRule(boq(token, projectId), "STEEL_RCC_STANDARD");
        mockMvc.perform(put("/api/boq/items/" + steelId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"itemName\": \"TMT Steel Fe500\", \"category\": \"MATERIAL\", \"unit\": \"kg\", \"quantity\": %s, \"rate\": %s, \"actualAmount\": 1000}"
                                .formatted(steel.path("quantity").asText(), steel.path("rate").asText())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estimateSource").value("ENGINEER_VALIDATED"));
    }

    @Test
    void validationIsTenantScoped() throws Exception {
        String tokenA = registerAndGetToken("Scoped A", "owner-validate-b@buildflow.test");
        String tokenB = registerAndGetToken("Scoped B", "owner-validate-c@buildflow.test");
        long[] ids = createEstimate(tokenA);
        long itemId = boq(tokenA, ids[1]).get(0).path("id").asLong();

        mockMvc.perform(post("/api/boq/items/" + itemId + "/validate")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(APPLICATION_JSON)
                        .content("{\"engineerName\": \"Intruder\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/projects/" + ids[1] + "/boq/validate")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(APPLICATION_JSON)
                        .content("{\"engineerName\": \"Intruder\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/boq/items/" + itemId + "/validate")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("{\"engineerName\": \" \"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    private JsonNode bySourceRule(JsonNode items, String ruleCode) {
        for (JsonNode item : items) {
            if (ruleCode.equals(item.path("sourceRuleCode").asText())) {
                return item;
            }
        }
        assertNotNull(null, "No line for rule " + ruleCode);
        return null;
    }

    private JsonNode boq(String token, long projectId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/projects/" + projectId + "/boq")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertTrue(data.size() > 0);
        return data;
    }

    // Returns {houseRequirementId, projectId} with a generated BOQ.
    private long[] createEstimate(String token) throws Exception {
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
        MvcResult requirement = mockMvc.perform(post("/api/house-requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"label": "30x50", "plotWidthFt": 30, "plotLengthFt": 50, "constructionGrade": "STANDARD",
                                 "floors": [{"floorLevel": 0, "floorAreaSqft": 1050, "doorCount": 6},
                                            {"floorLevel": 1, "floorAreaSqft": 750, "doorCount": 4}]}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long requirementId = objectMapper.readTree(requirement.getResponse().getContentAsString()).path("data").path("id").asLong();
        MvcResult generated = mockMvc.perform(post("/api/house-requirements/" + requirementId + "/generate-boq")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        long projectId = objectMapper.readTree(generated.getResponse().getContentAsString()).path("data").path("projectId").asLong();
        return new long[]{requirementId, projectId};
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
