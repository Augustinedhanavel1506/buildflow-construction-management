package com.buildflow.report;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void costSummaryUsesFlooredRemainingEstimateAndBoqVarianceTotalsAreCorrect() throws Exception {
        String tokenA = registerAndGetToken("Green Villa Constructions", "owner-rep-a@buildflow.test");
        String tokenB = registerAndGetToken("Skyline Commercial", "owner-rep-b@buildflow.test");

        long projectId = createProject(tokenA, "Green Villa", "5000000", "4200000");

        mockMvc.perform(post("/api/projects/" + projectId + "/expenses")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"category": "MATERIAL", "amount": 2840000, "date": "2026-09-15"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/projects/" + projectId + "/boq/items")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"itemName": "Cement", "category": "MATERIAL", "unit": "Bag", "quantity": 500, "rate": 420, "actualAmount": 235000}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/projects/" + projectId + "/reports/cost-summary")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actualCost").value(2840000.0))
                .andExpect(jsonPath("$.data.remainingEstimatedCost").value(1360000.0))
                .andExpect(jsonPath("$.data.estimatedFinalCost").value(4200000.0))
                .andExpect(jsonPath("$.data.estimatedMargin").value(800000.0))
                .andExpect(jsonPath("$.data.boqVariance.totalEstimated").value(210000.0))
                .andExpect(jsonPath("$.data.boqVariance.totalActual").value(235000.0))
                .andExpect(jsonPath("$.data.boqVariance.totalVariance").value(25000.0));

        mockMvc.perform(get("/api/projects/" + projectId + "/reports/cost-summary")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/projects/" + projectId + "/reports/boq-variance/export")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cement")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("TOTAL")));
    }

    @Test
    void profitabilityReportAggregatesAcrossProjectsAndFloorsFinalCostAtEstimate() throws Exception {
        String token = registerAndGetToken("Green Villa Constructions", "owner-rep-c@buildflow.test");

        createProject(token, "Green Villa", "5000000", "4200000");
        createProject(token, "Riverside Apartments", "8000000", "6500000");

        mockMvc.perform(get("/api/reports/profitability")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projects.length()").value(2))
                .andExpect(jsonPath("$.data.totalContractValue").value(13000000.0))
                .andExpect(jsonPath("$.data.totalEstimatedFinalCost").value(10700000.0))
                .andExpect(jsonPath("$.data.totalEstimatedMargin").value(2300000.0));

        mockMvc.perform(get("/api/reports/profitability/export")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Riverside Apartments")));
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
