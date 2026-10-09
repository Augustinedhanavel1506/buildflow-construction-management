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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WhatIfFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void scenariosAreComparedWithoutChangingThePlanOrTheBoq() throws Exception {
        String token = registerAndGetToken("What If Builders", "owner-whatif-a@buildflow.test");
        mockMvc.perform(post("/api/rate-master/starter").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        long id = createRequirement(token);

        // No changes: nothing differs.
        JsonNode same = whatIf(token, id, "{}");
        assertEquals(0, new BigDecimal("0").compareTo(new BigDecimal(same.path("difference").asText())));
        assertEquals(0, same.path("items").size());
        assertTrue(new BigDecimal(same.path("baseline").path("total").asText()).signum() > 0);

        // Dropping the first floor removes cost and area.
        JsonNode smaller = whatIf(token, id, "{\"removeFloorLevels\": [1]}");
        assertTrue(new BigDecimal(smaller.path("difference").asText()).signum() < 0);
        assertTrue(new BigDecimal(smaller.path("differencePercent").asText()).signum() < 0);
        assertEquals(1800.0, smaller.path("baseline").path("builtUpAreaSqft").asDouble());
        assertEquals(1050.0, smaller.path("scenario").path("builtUpAreaSqft").asDouble());
        assertTrue(smaller.path("components").size() > 0);
        assertTrue(smaller.path("items").size() > 0);
        // Largest changes come first.
        BigDecimal first = new BigDecimal(smaller.path("items").get(0).path("difference").asText()).abs();
        BigDecimal last = new BigDecimal(smaller.path("items").get(smaller.path("items").size() - 1).path("difference").asText()).abs();
        assertTrue(first.compareTo(last) >= 0);

        // A bigger ground floor costs more.
        JsonNode bigger = whatIf(token, id, "{\"floors\": [{\"floorLevel\": 0, \"floorAreaSqft\": 1150}]}");
        assertEquals(1900.0, bigger.path("scenario").path("builtUpAreaSqft").asDouble());
        assertTrue(new BigDecimal(bigger.path("difference").asText()).signum() > 0);

        // Upgrading the grade changes cost but not area.
        JsonNode premium = whatIf(token, id, "{\"constructionGrade\": \"PREMIUM\"}");
        assertEquals(1800.0, premium.path("scenario").path("builtUpAreaSqft").asDouble());
        assertTrue(new BigDecimal(premium.path("difference").asText()).signum() > 0);

        // Adding a floor adds area and cost.
        JsonNode taller = whatIf(token, id, "{\"addFloors\": [{\"floorAreaSqft\": 600, \"bedroomCount\": 1, \"bathroomCount\": 1}]}");
        assertEquals(2400.0, taller.path("scenario").path("builtUpAreaSqft").asDouble());
        assertTrue(new BigDecimal(taller.path("difference").asText()).signum() > 0);

        // Removing the balcony on the first floor saves a little and changes nothing else on that floor.
        JsonNode noBalcony = whatIf(token, id, "{\"floors\": [{\"floorLevel\": 1, \"hasBalcony\": false}]}");
        assertEquals(1800.0, noBalcony.path("scenario").path("builtUpAreaSqft").asDouble());

        // Nothing was saved: the plan and the project's BOQ are exactly as they were.
        mockMvc.perform(get("/api/house-requirements/" + id).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.constructionGrade").value("STANDARD"))
                .andExpect(jsonPath("$.data.floors.length()").value(2))
                .andExpect(jsonPath("$.data.floors[0].floorAreaSqft").value(1050.0));
        MvcResult requirement = mockMvc.perform(get("/api/house-requirements/" + id).header("Authorization", "Bearer " + token)).andReturn();
        long projectId = data(requirement).path("projectId").asLong();
        mockMvc.perform(get("/api/projects/" + projectId + "/boq").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void invalidScenariosAreRejectedAndScopedToTheBusiness() throws Exception {
        String token = registerAndGetToken("What If Guards", "owner-whatif-b@buildflow.test");
        String other = registerAndGetToken("What If Others", "owner-whatif-c@buildflow.test");
        mockMvc.perform(post("/api/rate-master/starter").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        long id = createRequirement(token);

        postWhatIf(token, id, "{\"floors\": [{\"floorLevel\": 9, \"floorAreaSqft\": 500}]}").andExpect(status().isBadRequest());
        postWhatIf(token, id, "{\"removeFloorLevels\": [0, 1]}").andExpect(status().isBadRequest());
        postWhatIf(token, id, "{\"removeFloorLevels\": [5]}").andExpect(status().isBadRequest());
        postWhatIf(token, id, "{\"floors\": [{\"floorLevel\": 0, \"floorAreaSqft\": -5}]}").andExpect(status().isUnprocessableEntity());
        postWhatIf(other, id, "{}").andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.ResultActions postWhatIf(String token, long id, String body) throws Exception {
        return mockMvc.perform(post("/api/house-requirements/" + id + "/what-if")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(body));
    }

    private JsonNode whatIf(String token, long id, String body) throws Exception {
        MvcResult result = postWhatIf(token, id, body).andExpect(status().isOk()).andReturn();
        return data(result);
    }

    private long createRequirement(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/house-requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"label": "30x50", "plotWidthFt": 30, "plotLengthFt": 50, "constructionGrade": "STANDARD",
                                 "floors": [
                                   {"floorLevel": 0, "floorAreaSqft": 1050, "bedroomCount": 2, "bathroomCount": 1,
                                    "hasKitchen": true, "hasHall": true, "doorCount": 6, "windowCount": 8},
                                   {"floorLevel": 1, "floorAreaSqft": 750, "bedroomCount": 1, "bathroomCount": 1,
                                    "hasBalcony": true, "doorCount": 4, "windowCount": 6}]}
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
