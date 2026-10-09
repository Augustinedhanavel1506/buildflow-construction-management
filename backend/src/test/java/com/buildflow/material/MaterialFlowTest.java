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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MaterialFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void purchaseAndUsageAdjustStockAndStatusReflectsMinimum() throws Exception {
        String token = registerAndGetToken("Green Villa Constructions", "owner-mat-a@buildflow.test");
        long projectId = createProject(token, "Green Villa");

        String materialPayload = """
                {"name": "Cement", "unit": "Bag", "minimumStock": 100, "openingStock": 0}
                """;

        MvcResult materialResult = mockMvc.perform(post("/api/projects/" + projectId + "/materials")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(materialPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("CRITICAL"))
                .andReturn();

        long materialId = objectMapper.readTree(materialResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/api/materials/" + materialId + "/purchases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"quantity": 200, "rate": 420, "supplierName": "ABC Traders", "purchaseDate": "2026-09-10"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(84000.0));

        mockMvc.perform(get("/api/projects/" + projectId + "/materials/purchases")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].materialName").value("Cement"))
                .andExpect(jsonPath("$.data[0].createdByName").value("Owner"));

        mockMvc.perform(get("/api/projects/" + projectId + "/materials")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].currentStock").value(200.0))
                .andExpect(jsonPath("$.data[0].status").value("NORMAL"));

        mockMvc.perform(post("/api/materials/" + materialId + "/usage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"quantity": 150, "usageDate": "2026-09-12", "notes": "Foundation work"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/projects/" + projectId + "/materials/usage")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].materialName").value("Cement"))
                .andExpect(jsonPath("$.data[0].createdByName").value("Owner"));

        mockMvc.perform(get("/api/projects/" + projectId + "/materials")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].currentStock").value(50.0))
                .andExpect(jsonPath("$.data[0].status").value("LOW"));

        mockMvc.perform(post("/api/materials/" + materialId + "/usage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"quantity": 999, "usageDate": "2026-09-13"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void materialRequestApprovalWorkflowAndTenantIsolation() throws Exception {
        String tokenA = registerAndGetToken("Green Villa Constructions", "owner-mat-b@buildflow.test");
        String tokenB = registerAndGetToken("Skyline Commercial", "owner-mat-c@buildflow.test");

        long projectId = createProject(tokenA, "Green Villa");

        MvcResult materialResult = mockMvc.perform(post("/api/projects/" + projectId + "/materials")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "Steel", "unit": "Kg", "minimumStock": 500, "openingStock": 100}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        long materialId = objectMapper.readTree(materialResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        MvcResult requestResult = mockMvc.perform(post("/api/projects/" + projectId + "/materials/requests")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"materialId": %d, "quantity": 100, "requiredDate": "2026-09-20", "reason": "Running low"}
                                """.formatted(materialId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        long requestId = objectMapper.readTree(requestResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/projects/" + projectId + "/materials/requests")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].materialName").value("Steel"))
                .andExpect(jsonPath("$.data[0].requestedByName").value("Owner"));

        mockMvc.perform(patch("/api/materials/requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/materials/requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(patch("/api/materials/requests/" + requestId + "/reject")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
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
