package com.buildflow.tally;

import com.buildflow.auth.dto.RegisterRequest;
import org.hamcrest.Matchers;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TallyExportFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void salesVoucherXmlIncludesOneVoucherPerInvoiceWithBalancedLedgerEntries() throws Exception {
        String token = registerAndGetToken("Coastal Builders", "owner-tally-a@buildflow.test");

        MvcResult projResult = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "Coastal Apartments", "contractValue": 5000000, "clientName": "ABC Developers"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long projectId = objectMapper.readTree(projResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

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

        mockMvc.perform(post("/api/billing/ra-bills/" + billId + "/gst-invoice")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"invoiceNumber": "INV-1", "invoiceDate": "2026-09-05", "interState": false, "gstRate": 18}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tally/export/sales-vouchers")
                        .param("from", "2026-09-01")
                        .param("to", "2026-09-30")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("<TALLYREQUEST>Import Data</TALLYREQUEST>")))
                .andExpect(content().string(Matchers.containsString("<VOUCHERNUMBER>INV-1</VOUCHERNUMBER>")))
                .andExpect(content().string(Matchers.containsString("<PARTYLEDGERNAME>ABC Developers</PARTYLEDGERNAME>")))
                .andExpect(content().string(Matchers.containsString("<DATE>20260905</DATE>")))
                // Party debited for the full invoice value (200000 taxable + 36000 GST).
                .andExpect(content().string(Matchers.containsString("<AMOUNT>-236000.00</AMOUNT>")))
                .andExpect(content().string(Matchers.containsString("<LEDGERNAME>Sales - Construction Services</LEDGERNAME>")))
                .andExpect(content().string(Matchers.containsString("<AMOUNT>200000.00</AMOUNT>")))
                .andExpect(content().string(Matchers.containsString("<LEDGERNAME>Output CGST</LEDGERNAME>")))
                .andExpect(content().string(Matchers.containsString("<LEDGERNAME>Output SGST</LEDGERNAME>")))
                .andExpect(content().string(Matchers.containsString("<AMOUNT>18000.00</AMOUNT>")));

        // Outside the date range, no vouchers should be included.
        mockMvc.perform(get("/api/tally/export/sales-vouchers")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString("INV-1"))));
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
