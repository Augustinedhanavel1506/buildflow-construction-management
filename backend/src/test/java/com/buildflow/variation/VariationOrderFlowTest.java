package com.buildflow.variation;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VariationOrderFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void approvedVariationsRevisesContractValueEverywhereRejectedOnesDont() throws Exception {
        String tokenA = registerAndGetToken("Green Villa Constructions", "owner-vo-a@buildflow.test");
        String tokenB = registerAndGetToken("Skyline Commercial", "owner-vo-b@buildflow.test");

        long projectId = createProject(tokenA, "Green Villa", "5000000", "4200000");

        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contractValue").value(5000000.0))
                .andExpect(jsonPath("$.data.revisedContractValue").value(5000000.0));

        long additionId = createVariation(tokenA, projectId, "Extra bathroom", "ADDITION", "300000");
        long omissionId = createVariation(tokenA, projectId, "Drop garden wall", "OMISSION", "50000");
        long pendingId = createVariation(tokenA, projectId, "Undecided change", "ADDITION", "1000000");

        mockMvc.perform(patch("/api/variations/" + additionId + "/approve")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/variations/" + additionId + "/approve")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(patch("/api/variations/" + omissionId + "/approve")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/variations/" + additionId + "/reject")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());

        // Revised contract value = 5,000,000 + 300,000 (approved addition) - 50,000 (approved omission)
        // The still-PENDING 1,000,000 addition must not count yet.
        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contractValue").value(5000000.0))
                .andExpect(jsonPath("$.data.revisedContractValue").value(5250000.0));

        mockMvc.perform(get("/api/projects/" + projectId + "/reports/cost-summary")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revisedContractValue").value(5250000.0))
                .andExpect(jsonPath("$.data.estimatedMargin").value(1050000.0));

        mockMvc.perform(get("/api/reports/profitability")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projects[0].revisedContractValue").value(5250000.0));

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.financialSummary.totalContractValue").value(5250000.0));

        mockMvc.perform(patch("/api/variations/" + pendingId + "/reject")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revisedContractValue").value(5250000.0));

        mockMvc.perform(get("/api/projects/" + projectId + "/variations")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    private long createVariation(String token, long projectId, String title, String type, String amount) throws Exception {
        String payload = """
                {"title": "%s", "type": "%s", "amount": %s}
                """.formatted(title, type, amount);

        MvcResult result = mockMvc.perform(post("/api/projects/" + projectId + "/variations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private long createProject(String token, String name, String contractValue, String estimatedCost) throws Exception {
        String payload = """
                {"name": "%s", "contractValue": %s, "estimatedCost": %s}
                """.formatted(name, contractValue, estimatedCost);

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
