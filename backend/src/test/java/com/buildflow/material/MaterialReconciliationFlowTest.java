package com.buildflow.material;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MaterialReconciliationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void physicalCountDiscrepancySurfacesAsEstimatedLossInReportAndDashboard() throws Exception {
        String tokenA = registerAndGetToken("Green Villa Constructions", "owner-recon-a@buildflow.test");
        String tokenB = registerAndGetToken("Skyline Commercial", "owner-recon-b@buildflow.test");

        long projectId = createProject(tokenA, "Green Villa");

        MvcResult materialResult = mockMvc.perform(post("/api/projects/" + projectId + "/materials")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "Cement", "unit": "Bag", "minimumStock": 50, "openingStock": 0}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        long materialId = objectMapper.readTree(materialResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/api/materials/" + materialId + "/purchases")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"quantity": 200, "rate": 420, "purchaseDate": "2026-09-01"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/materials/" + materialId + "/usage")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"quantity": 50, "usageDate": "2026-09-05"}
                                """))
                .andExpect(status().isCreated());

        // System now expects 150 bags on site. A physical count finds only 140 — a 10-bag shortage.
        mockMvc.perform(post("/api/materials/" + materialId + "/stock-counts")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"countDate": "2026-09-10", "countedStock": 140}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/materials/" + materialId + "/stock-counts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"countDate": "2026-09-10", "countedStock": 140, "notes": "Monthly audit"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.systemStock").value(150.0))
                .andExpect(jsonPath("$.data.variance").value(-10.0));

        // The count trues up current stock to the physical reality.
        mockMvc.perform(get("/api/projects/" + projectId + "/materials")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].currentStock").value(140.0));

        mockMvc.perform(get("/api/projects/" + projectId + "/materials/reconciliation")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.materials[0].totalPurchased").value(200.0))
                .andExpect(jsonPath("$.data.materials[0].totalUsed").value(50.0))
                .andExpect(jsonPath("$.data.materials[0].averageRate").value(420.0))
                .andExpect(jsonPath("$.data.materials[0].cumulativeVarianceQty").value(-10.0))
                .andExpect(jsonPath("$.data.materials[0].estimatedVarianceValue").value(-4200.0))
                .andExpect(jsonPath("$.data.totalEstimatedVarianceValue").value(-4200.0));

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estimatedMaterialVarianceValue").value(-4200.0));

        mockMvc.perform(get("/api/projects/" + projectId + "/materials/reconciliation")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    private long createProject(String token, String name) throws Exception {
        String payload = """
                {"name": "%s", "contractValue": 5000000}
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
