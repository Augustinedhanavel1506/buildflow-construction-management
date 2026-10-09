package com.buildflow.ratemaster;

import com.buildflow.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RateMasterItemFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void rateMasterIsSharedAcrossProjectsAndIsolatedByBusinessAndFiltersInactive() throws Exception {
        String tokenA = registerAndGetToken("Green Villa Constructions", "owner-rm-a@buildflow.test");
        String tokenB = registerAndGetToken("Skyline Commercial", "owner-rm-b@buildflow.test");

        MvcResult createResult = mockMvc.perform(post("/api/rate-master")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"itemName": "Cement OPC 53 Grade", "category": "MATERIAL", "unit": "Bag", "standardRate": 420}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.active").value(true))
                .andReturn();

        long itemId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/rate-master")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].itemName").value("Cement OPC 53 Grade"));

        // Business B must not see business A's rate master.
        mockMvc.perform(get("/api/rate-master")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(put("/api/rate-master/" + itemId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"itemName": "Hijacked", "category": "MATERIAL", "unit": "Bag", "standardRate": 1}
                                """))
                .andExpect(status().isNotFound());

        // Deactivate the item; it should disappear from the default (active-only) list.
        mockMvc.perform(put("/api/rate-master/" + itemId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"itemName": "Cement OPC 53 Grade", "category": "MATERIAL", "unit": "Bag", "standardRate": 440, "active": false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.standardRate").value(440.0));

        mockMvc.perform(get("/api/rate-master")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get("/api/rate-master")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("includeInactive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].active").value(false));
    }

    @Test
    void priceBandAndConsumptionCoefficientAreStoredAndMinMustNotExceedMax() throws Exception {
        String token = registerAndGetToken("Coastal Builders", "owner-rm-c@buildflow.test");

        mockMvc.perform(post("/api/rate-master")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"itemName": "Cement (Standard PPC)", "category": "MATERIAL", "unit": "Bag",
                                 "standardRate": 390, "minRate": 370, "maxRate": 411,
                                 "consumptionPerSqft": 0.4, "notes": "Rates drop on bulk orders exceeding 50 bags."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.minRate").value(370.0))
                .andExpect(jsonPath("$.data.maxRate").value(411.0))
                .andExpect(jsonPath("$.data.consumptionPerSqft").value(0.4))
                .andExpect(jsonPath("$.data.notes").value("Rates drop on bulk orders exceeding 50 bags."));

        // A min rate above the max rate is rejected rather than silently stored.
        mockMvc.perform(post("/api/rate-master")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"itemName": "M-Sand", "category": "MATERIAL", "unit": "Tonne",
                                 "standardRate": 1350, "minRate": 1500, "maxRate": 1200}
                                """))
                .andExpect(status().isBadRequest());
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
