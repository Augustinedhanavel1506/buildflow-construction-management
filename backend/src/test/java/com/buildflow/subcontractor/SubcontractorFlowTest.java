package com.buildflow.subcontractor;

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
class SubcontractorFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void subcontractorPaymentFlowsIntoExpensesAndBillTransitionsToPaid() throws Exception {
        String tokenA = registerAndGetToken("Green Villa Constructions", "owner-sub-a@buildflow.test");
        String tokenB = registerAndGetToken("Skyline Commercial", "owner-sub-b@buildflow.test");

        long projectId = createProject(tokenA, "Green Villa");

        MvcResult subResult = mockMvc.perform(post("/api/subcontractors")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "Sharma Electricals", "tradeType": "Electrical", "phone": "9876543210"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long subcontractorId = objectMapper.readTree(subResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Business B must never see business A's subcontractor master.
        mockMvc.perform(get("/api/subcontractors").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        MvcResult woResult = mockMvc.perform(post("/api/projects/" + projectId + "/subcontract-work-orders")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"subcontractorId": %d, "title": "Full electrical wiring", "contractType": "LUMP_SUM", "contractValue": 500000}
                                """.formatted(subcontractorId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.subcontractorName").value("Sharma Electricals"))
                .andReturn();
        long workOrderId = objectMapper.readTree(woResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        MvcResult billResult = mockMvc.perform(post("/api/subcontract-work-orders/" + workOrderId + "/bills")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"billNumber": "SC-1", "billDate": "2026-09-19", "workDoneValue": 200000, "tdsPercent": 2, "retentionPercent": 5}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.tdsAmount").value(4000.0))
                .andExpect(jsonPath("$.data.retentionAmount").value(10000.0))
                .andExpect(jsonPath("$.data.netPayableAmount").value(186000.0))
                .andReturn();
        long billId = objectMapper.readTree(billResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Payments cannot be recorded against a draft bill.
        mockMvc.perform(post("/api/subcontractor-bills/" + billId + "/payments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"amount": 1000, "paymentDate": "2026-09-19", "mode": "BANK_TRANSFER"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/subcontractor-bills/" + billId + "/approve")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/subcontractor-bills/" + billId + "/approve")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/api/projects/" + projectId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actualCost").value(0));

        mockMvc.perform(post("/api/subcontractor-bills/" + billId + "/payments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"amount": 186000, "paymentDate": "2026-09-20", "mode": "BANK_TRANSFER", "referenceNumber": "TXN99"}
                                """))
                .andExpect(status().isCreated());

        // Fully paid — the bill status flips, and the payment shows up as a real project expense.
        mockMvc.perform(get("/api/subcontract-work-orders/" + workOrderId + "/bills")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("PAID"))
                .andExpect(jsonPath("$.data[0].outstandingAmount").value(0.0));

        mockMvc.perform(get("/api/projects/" + projectId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actualCost").value(186000.0));

        mockMvc.perform(get("/api/projects/" + projectId + "/expenses").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].category").value("SUBCONTRACTOR"))
                .andExpect(jsonPath("$.data[0].amount").value(186000.0))
                .andExpect(jsonPath("$.data[0].supplierName").value("Sharma Electricals"));

        mockMvc.perform(get("/api/projects/" + projectId + "/subcontract-work-orders")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].totalBilled").value(186000.0))
                .andExpect(jsonPath("$.data[0].totalPaid").value(186000.0));

        mockMvc.perform(get("/api/projects/" + projectId + "/subcontract-work-orders")
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
