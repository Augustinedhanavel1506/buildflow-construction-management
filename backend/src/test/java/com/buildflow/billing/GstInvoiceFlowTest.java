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
class GstInvoiceFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void gstInvoiceIsGeneratedFromASubmittedBillWithCorrectIntraStateAndInterStateSplit() throws Exception {
        String tokenA = registerAndGetToken("Green Villa Constructions", "owner-gst-a@buildflow.test");
        String tokenB = registerAndGetToken("Skyline Commercial", "owner-gst-b@buildflow.test");

        mockMvc.perform(put("/api/business")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "Green Villa Constructions", "gstin": "27AAAAA0000A1Z5", "stateName": "Maharashtra", "defaultGstRate": 18}
                                """))
                .andExpect(status().isOk());

        long projectId = createProject(tokenA, "Green Villa", "29BBBBB0000B1Z1", "Bengaluru, Karnataka");

        MvcResult billResult = mockMvc.perform(post("/api/projects/" + projectId + "/billing/ra-bills")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"billNumber": "RA-1", "billDate": "2026-09-01", "workDoneValue": 1000000, "retentionPercent": 5, "otherDeductions": 0}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        long billId = objectMapper.readTree(billResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Cannot issue a tax invoice against a bill still in DRAFT.
        mockMvc.perform(post("/api/billing/ra-bills/" + billId + "/gst-invoice")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"invoiceNumber": "INV-1", "invoiceDate": "2026-09-02", "interState": false}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/billing/ra-bills/" + billId + "/submit")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Client (Bengaluru, Karnataka) differs from supplier's state (Maharashtra) -> inter-state supply, full IGST.
        MvcResult invoiceResult = mockMvc.perform(post("/api/billing/ra-bills/" + billId + "/gst-invoice")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"invoiceNumber": "INV-1", "invoiceDate": "2026-09-02", "interState": true, "placeOfSupply": "Karnataka"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.taxableValue").value(1000000.0))
                .andExpect(jsonPath("$.data.gstRate").value(18.0))
                .andExpect(jsonPath("$.data.igstAmount").value(180000.0))
                .andExpect(jsonPath("$.data.cgstAmount").value(0.0))
                .andExpect(jsonPath("$.data.sgstAmount").value(0.0))
                .andExpect(jsonPath("$.data.totalInvoiceValue").value(1180000.0))
                .andExpect(jsonPath("$.data.hsnSacCode").value("9954"))
                .andExpect(jsonPath("$.data.supplierGstin").value("27AAAAA0000A1Z5"))
                .andExpect(jsonPath("$.data.clientGstin").value("29BBBBB0000B1Z1"))
                .andReturn();

        long invoiceId = objectMapper.readTree(invoiceResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // A bill can only ever have one tax invoice issued against it.
        mockMvc.perform(post("/api/billing/ra-bills/" + billId + "/gst-invoice")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"invoiceNumber": "INV-2", "invoiceDate": "2026-09-03", "interState": true}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/billing/ra-bills/" + billId + "/gst-invoice")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invoiceNumber").value("INV-1"));

        mockMvc.perform(get("/api/billing/gst-invoices/" + invoiceId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.billNumber").value("RA-1"))
                .andExpect(jsonPath("$.data.projectName").value("Green Villa"));

        mockMvc.perform(get("/api/billing/gst-invoices")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].invoiceNumber").value("INV-1"));

        // Tenant isolation: business B cannot see business A's bill or its invoice.
        mockMvc.perform(get("/api/billing/ra-bills/" + billId + "/gst-invoice")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/billing/gst-invoices/" + invoiceId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void intraStateSupplySplitsTaxEquallyBetweenCgstAndSgst() throws Exception {
        String token = registerAndGetToken("Coastal Builders", "owner-gst-c@buildflow.test");

        long projectId = createProject(token, "Coastal Apartments", null, null);

        MvcResult billResult = mockMvc.perform(post("/api/projects/" + projectId + "/billing/ra-bills")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"billNumber": "RA-1", "billDate": "2026-09-01", "workDoneValue": 200000, "retentionPercent": 0, "otherDeductions": 0}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        long billId = objectMapper.readTree(billResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(patch("/api/billing/ra-bills/" + billId + "/submit")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // No business default GST rate was configured, so the request-level rate is used.
        mockMvc.perform(post("/api/billing/ra-bills/" + billId + "/gst-invoice")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"invoiceNumber": "INV-1", "invoiceDate": "2026-09-02", "interState": false, "gstRate": 18}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.cgstAmount").value(18000.0))
                .andExpect(jsonPath("$.data.sgstAmount").value(18000.0))
                .andExpect(jsonPath("$.data.igstAmount").value(0.0))
                .andExpect(jsonPath("$.data.totalInvoiceValue").value(236000.0));
    }

    private long createProject(String token, String name, String clientGstin, String clientAddress) throws Exception {
        String payload = """
                {"name": "%s", "contractValue": 5000000, "clientGstin": %s, "clientAddress": %s}
                """.formatted(
                        name,
                        clientGstin == null ? "null" : "\"" + clientGstin + "\"",
                        clientAddress == null ? "null" : "\"" + clientAddress + "\"");

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
