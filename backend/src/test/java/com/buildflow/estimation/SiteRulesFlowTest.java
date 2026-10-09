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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SiteRulesFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void siteCheckMeasuresCoverageFarAndSetbackIntrusionAgainstTheRulesEntered() throws Exception {
        String token = registerAndGetToken("Site Builders", "owner-site-a@buildflow.test");
        long id = createRequirement(token);

        // Nothing entered: setbacks are the planning defaults, limits are not checked.
        mockMvc.perform(get("/api/house-requirements/" + id + "/site-rules").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.setbacks.frontFt").value(5.0))
                .andExpect(jsonPath("$.data.setbacks.frontAssumed").value(true))
                .andExpect(jsonPath("$.data.coverage.status").value("NOT_SET"))
                .andExpect(jsonPath("$.data.far.status").value("NOT_SET"))
                .andExpect(jsonPath("$.data.buildableEnvelope.widthFt").value(24.0))
                .andExpect(jsonPath("$.data.buildableEnvelope.depthFt").value(42.0))
                .andExpect(jsonPath("$.data.fullyDrawn").value(false));

        // Ground floor drawn as two 10 x 10 rooms starting inside the envelope (x 3, y 5); first floor stays on the checklist.
        mockMvc.perform(put("/api/house-requirements/" + id + "/plans/0")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"rooms": [
                                  {"id": "a", "type": "HALL", "name": "Hall", "x": 3, "y": 5, "widthFt": 10, "depthFt": 30},
                                  {"id": "b", "type": "BEDROOM", "name": "Bedroom", "x": 13, "y": 5, "widthFt": 10, "depthFt": 30}]}
                                """))
                .andExpect(status().isOk());

        // Ground 600 sq.ft of 1,500 = 40.0%; total 600 + 450 (checklist) = 1,050 = FAR 0.70.
        mockMvc.perform(put("/api/house-requirements/" + id + "/site-rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"maxCoveragePercent\": 50, \"maxFar\": 0.6}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groundAreaSqft").value(600.0))
                .andExpect(jsonPath("$.data.totalAreaSqft").value(1050.0))
                .andExpect(jsonPath("$.data.coverage.value").value(40.0))
                .andExpect(jsonPath("$.data.coverage.status").value("OK"))
                .andExpect(jsonPath("$.data.far.value").value(0.7))
                .andExpect(jsonPath("$.data.far.status").value("EXCEEDS"))
                .andExpect(jsonPath("$.data.compliant").value(false))
                .andExpect(jsonPath("$.data.violations.length()").value(0));

        // Raising the FAR limit and tightening coverage flips the two results.
        mockMvc.perform(put("/api/house-requirements/" + id + "/site-rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"maxCoveragePercent\": 35, \"maxFar\": 1.5}"))
                .andExpect(jsonPath("$.data.coverage.status").value("EXCEEDS"))
                .andExpect(jsonPath("$.data.far.status").value("OK"))
                .andExpect(jsonPath("$.data.compliant").value(false));

        // A bigger front setback than the drawn room allows is reported against that room, by side.
        mockMvc.perform(put("/api/house-requirements/" + id + "/site-rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"setbackFrontFt\": 8, \"setbackLeftFt\": 4, \"maxCoveragePercent\": 60, \"maxFar\": 1.5}"))
                .andExpect(jsonPath("$.data.setbacks.frontAssumed").value(false))
                .andExpect(jsonPath("$.data.setbacks.rearAssumed").value(true))
                .andExpect(jsonPath("$.data.violations.length()").value(2))
                .andExpect(jsonPath("$.data.violations[0].roomName").value("Hall"))
                .andExpect(jsonPath("$.data.violations[0].message").value("extends into the front setback and the left setback"))
                .andExpect(jsonPath("$.data.violations[1].roomName").value("Bedroom"))
                .andExpect(jsonPath("$.data.violations[1].message").value("extends into the front setback"))
                .andExpect(jsonPath("$.data.compliant").value(false));

        // Back to defaults and generous limits: compliant.
        mockMvc.perform(put("/api/house-requirements/" + id + "/site-rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"maxCoveragePercent\": 60, \"maxFar\": 1.5}"))
                .andExpect(jsonPath("$.data.violations.length()").value(0))
                .andExpect(jsonPath("$.data.compliant").value(true));

        // Setbacks that leave no room are called out.
        MvcResult tight = mockMvc.perform(put("/api/house-requirements/" + id + "/site-rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"setbackLeftFt\": 20, \"setbackRightFt\": 20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.compliant").value(false))
                .andReturn();
        assertTrue(objectMapper.readTree(tight.getResponse().getContentAsString()).path("data").path("notes").toString().contains("no buildable area"));
    }

    @Test
    void suggestedLayoutsKeepInsideTheSetbacksAndRulesAreValidatedAndScoped() throws Exception {
        String token = registerAndGetToken("Site Guards", "owner-site-b@buildflow.test");
        String other = registerAndGetToken("Site Others", "owner-site-c@buildflow.test");
        long id = createRequirement(token);

        mockMvc.perform(put("/api/house-requirements/" + id + "/site-rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"setbackFrontFt\": 10, \"setbackRearFt\": 6, \"setbackLeftFt\": 5, \"setbackRightFt\": 5}"))
                .andExpect(status().isOk());

        MvcResult suggested = mockMvc.perform(post("/api/house-requirements/" + id + "/plans/suggest")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode floor : objectMapper.readTree(suggested.getResponse().getContentAsString()).path("data").path("floors")) {
            for (JsonNode room : floor.path("rooms")) {
                assertTrue(room.path("x").asDouble() >= 5 - 0.01, "left setback respected");
                assertTrue(room.path("y").asDouble() >= 10 - 0.01, "front setback respected");
                assertTrue(room.path("x").asDouble() + room.path("widthFt").asDouble() <= 25 + 0.01, "right setback respected");
                assertTrue(room.path("y").asDouble() + room.path("depthFt").asDouble() <= 44 + 0.01, "rear setback respected");
            }
        }

        mockMvc.perform(put("/api/house-requirements/" + id + "/site-rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"setbackFrontFt\": -2}"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(put("/api/house-requirements/" + id + "/site-rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"maxCoveragePercent\": 150}"))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/api/house-requirements/" + id + "/site-rules").header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/house-requirements/" + id + "/site-rules")
                        .header("Authorization", "Bearer " + other)
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
        assertEquals(10.0, objectMapper.readTree(mockMvc.perform(get("/api/house-requirements/" + id + "/site-rules")
                        .header("Authorization", "Bearer " + token)).andReturn().getResponse().getContentAsString())
                .path("data").path("setbacks").path("frontFt").asDouble());
    }

    private long createRequirement(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/house-requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"label": "30x50", "plotWidthFt": 30, "plotLengthFt": 50, "constructionGrade": "STANDARD",
                                 "floors": [
                                   {"floorLevel": 0, "floorAreaSqft": 600, "bedroomCount": 1, "hasHall": true},
                                   {"floorLevel": 1, "floorAreaSqft": 450, "bedroomCount": 1}]}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private String registerAndGetToken(String businessName, String email) throws Exception {
        RegisterRequest request = new RegisterRequest(businessName, "Owner", email, "SecurePass123");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("accessToken").asText();
    }
}
