package com.buildflow.dashboard;

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
class DashboardFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void dashboardAggregatesAcrossProjectsAndIsolatesByBusiness() throws Exception {
        String tokenA = registerAndGetToken("Green Villa Constructions", "owner-dash-a@buildflow.test");
        String tokenB = registerAndGetToken("Skyline Commercial", "owner-dash-b@buildflow.test");

        long project1 = createProject(tokenA, "Green Villa", "5000000", "4200000");
        long project2 = createProject(tokenA, "Riverside Apartments", "3000000", "2500000");

        mockMvc.perform(post("/api/projects/" + project1 + "/expenses")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"category": "MATERIAL", "amount": 100000, "date": "2026-09-15"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/projects/" + project2 + "/expenses")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"category": "LABOUR", "amount": 50000, "date": "2026-09-16"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/projects/" + project1 + "/daily-reports")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"reportDate": "2026-09-16", "workersPresent": 10}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/projects/" + project1 + "/progress")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"stageName": "Foundation", "percentComplete": 100}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/projects/" + project1 + "/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectCounts.totalProjects").value(2))
                .andExpect(jsonPath("$.data.projectCounts.activeProjects").value(1))
                .andExpect(jsonPath("$.data.financialSummary.totalContractValue").value(8000000.0))
                .andExpect(jsonPath("$.data.financialSummary.totalEstimatedCost").value(6700000.0))
                .andExpect(jsonPath("$.data.financialSummary.totalActualCost").value(150000.0))
                .andExpect(jsonPath("$.data.overallProgress").value(50))
                .andExpect(jsonPath("$.data.costBreakdown.length()").value(2))
                .andExpect(jsonPath("$.data.recentActivity.length()").value(3));

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectCounts.totalProjects").value(0))
                .andExpect(jsonPath("$.data.financialSummary.totalActualCost").value(0));
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
