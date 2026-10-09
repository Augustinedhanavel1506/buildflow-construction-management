package com.buildflow.expense;

import com.buildflow.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExpenseFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void recordingExpensesUpdatesProjectActualCostAndEnforcesTenantIsolation() throws Exception {
        String tokenA = registerAndGetToken("Green Villa Constructions", "owner-exp-a@buildflow.test");
        String tokenB = registerAndGetToken("Skyline Commercial", "owner-exp-b@buildflow.test");

        long projectId = createProject(tokenA, "Green Villa");

        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actualCost").value(0));

        String expensePayload = """
                {
                  "category": "MATERIAL",
                  "amount": 45000,
                  "date": "2026-09-17",
                  "supplierName": "ABC Cement Traders",
                  "description": "Cement delivery"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/projects/" + projectId + "/expenses")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content(expensePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.createdByName").value("Owner"))
                .andReturn();

        long expenseId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actualCost").value(45000.0));

        mockMvc.perform(get("/api/projects/" + projectId + "/expenses")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].createdByName").value("Owner"));

        mockMvc.perform(get("/api/expenses/" + expenseId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdByName").value("Owner"));

        mockMvc.perform(get("/api/projects/" + projectId + "/expenses")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/expenses/" + expenseId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/expenses/" + expenseId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actualCost").value(0));
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
