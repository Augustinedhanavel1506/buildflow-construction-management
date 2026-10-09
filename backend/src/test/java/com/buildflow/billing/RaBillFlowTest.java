package com.buildflow.billing;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RaBillFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void raBillLifecycleFromDraftToPaidWithRetentionAndPartialPayments() throws Exception {
        String tokenA = registerAndGetToken("Green Villa Constructions", "owner-bill-a@buildflow.test");
        String tokenB = registerAndGetToken("Skyline Commercial", "owner-bill-b@buildflow.test");

        long projectId = createProject(tokenA, "Green Villa");

        String draftPayload = """
                {"billNumber": "RA-1", "billDate": "2026-09-01", "workDoneValue": 1000000, "retentionPercent": 5, "otherDeductions": 0}
                """;

        MvcResult createResult = mockMvc.perform(post("/api/projects/" + projectId + "/billing/ra-bills")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content(draftPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.retentionAmount").value(50000.0))
                .andExpect(jsonPath("$.data.netPayableAmount").value(950000.0))
                .andReturn();

        long billId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Only DRAFT bills can be edited.
        mockMvc.perform(put("/api/billing/ra-bills/" + billId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content(draftPayload.replace("1000000", "1100000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.netPayableAmount").value(1045000.0));

        mockMvc.perform(patch("/api/billing/ra-bills/" + billId + "/submit")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/billing/ra-bills/" + billId + "/submit")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));

        // Once submitted, raw fields are locked.
        mockMvc.perform(put("/api/billing/ra-bills/" + billId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content(draftPayload))
                .andExpect(status().isBadRequest());

        // Client certifies a lower amount than claimed; retention/net recompute off the certified value.
        mockMvc.perform(patch("/api/billing/ra-bills/" + billId + "/certify")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"certifiedAmount": 1000000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CERTIFIED"))
                .andExpect(jsonPath("$.data.retentionAmount").value(50000.0))
                .andExpect(jsonPath("$.data.netPayableAmount").value(950000.0));

        // Record a partial payment against the certified bill.
        mockMvc.perform(post("/api/billing/ra-bills/" + billId + "/payments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"amount": 500000, "paymentDate": "2026-09-10", "mode": "BANK_TRANSFER", "referenceNumber": "TXN123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(500000.0));

        mockMvc.perform(get("/api/projects/" + projectId + "/billing/ra-bills")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].amountReceived").value(500000.0))
                .andExpect(jsonPath("$.data[0].outstandingAmount").value(450000.0))
                .andExpect(jsonPath("$.data[0].status").value("CERTIFIED"));

        // Final payment should auto-transition the bill to PAID.
        mockMvc.perform(post("/api/billing/ra-bills/" + billId + "/payments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"amount": 450000, "paymentDate": "2026-09-20", "mode": "UPI"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/projects/" + projectId + "/billing/ra-bills")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("PAID"))
                .andExpect(jsonPath("$.data[0].outstandingAmount").value(0.0));

        // Fully paid bills reject further payments.
        mockMvc.perform(post("/api/billing/ra-bills/" + billId + "/payments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"amount": 1000, "paymentDate": "2026-09-21", "mode": "CASH"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/projects/" + projectId + "/billing/summary")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBilled").value(950000.0))
                .andExpect(jsonPath("$.data.totalReceived").value(950000.0))
                .andExpect(jsonPath("$.data.totalOutstanding").value(0.0))
                .andExpect(jsonPath("$.data.totalRetentionHeld").value(50000.0));

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.billingSummary.totalRetentionHeld").value(50000.0));

        mockMvc.perform(get("/api/projects/" + projectId + "/billing/ra-bills")
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
