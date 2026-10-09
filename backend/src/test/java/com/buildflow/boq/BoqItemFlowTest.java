package com.buildflow.boq;

import com.buildflow.auth.dto.RegisterRequest;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BoqItemFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void estimatedAmountIsServerCalculatedAndVarianceReflectsActuals() throws Exception {
        String tokenA = registerAndGetToken("Green Villa Constructions", "owner-boq-a@buildflow.test");
        String tokenB = registerAndGetToken("Skyline Commercial", "owner-boq-b@buildflow.test");

        long projectId = createProject(tokenA, "Green Villa");

        String createItemPayload = """
                {
                  "itemName": "Cement",
                  "category": "MATERIAL",
                  "unit": "Bag",
                  "quantity": 500,
                  "rate": 420,
                  "actualAmount": 235000
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/projects/" + projectId + "/boq/items")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content(createItemPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.estimatedAmount").value(210000.0))
                .andExpect(jsonPath("$.data.actualAmount").value(235000.0))
                .andExpect(jsonPath("$.data.variance").value(25000.0))
                .andReturn();

        long itemId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/projects/" + projectId + "/boq")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].itemName").value("Cement"));

        mockMvc.perform(get("/api/projects/" + projectId + "/boq")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/boq/items/" + itemId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(APPLICATION_JSON)
                        .content(createItemPayload))
                .andExpect(status().isNotFound());
    }

    private long createProject(String token, String name) throws Exception {
        String payload = """
                {
                  "name": "%s",
                  "contractValue": 5000000
                }
                """.formatted(name);

        MvcResult result = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
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
