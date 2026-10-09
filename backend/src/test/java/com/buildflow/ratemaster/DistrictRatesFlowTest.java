package com.buildflow.ratemaster;

import com.buildflow.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DistrictRatesFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void districtRatesOverrideDefaultsInEstimatesAndCopyingScalesWithoutOverwriting() throws Exception {
        String token = registerAndGetToken("District Builders", "owner-district-a@buildflow.test");
        String other = registerAndGetToken("District Others", "owner-district-b@buildflow.test");
        mockMvc.perform(post("/api/rate-master/starter").header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        // One rate per item and district.
        mockMvc.perform(post("/api/rate-master")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"itemName\": \"opc 53 grade cement\", \"category\": \"MATERIAL\", \"unit\": \"bag\", \"standardRate\": 1}"))
                .andExpect(status().isBadRequest());

        // A Madurai-specific cement rate is allowed alongside the default.
        mockMvc.perform(post("/api/rate-master")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"itemName\": \"OPC 53 Grade Cement\", \"category\": \"MATERIAL\", \"unit\": \"bag\", \"standardRate\": 500, \"district\": \"Madurai\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.district").value("Madurai"));
        mockMvc.perform(post("/api/rate-master")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"itemName\": \"OPC 53 Grade Cement\", \"category\": \"MATERIAL\", \"unit\": \"bag\", \"standardRate\": 501, \"district\": \"madurai\"}"))
                .andExpect(status().isBadRequest());

        long defaultRequirement = createRequirement(token, "");
        long maduraiRequirement = createRequirement(token, "\"district\": \"Madurai\",");
        BigDecimal defaultRate = cementRate(generate(token, defaultRequirement));
        BigDecimal maduraiRate = cementRate(generate(token, maduraiRequirement));
        assertEquals(0, new BigDecimal("420").compareTo(defaultRate), "No district uses the default rate.");
        assertEquals(0, new BigDecimal("500").compareTo(maduraiRate), "The district rate wins.");

        // Items without a Madurai rate still fall back to their default, so nothing goes unpriced.
        assertEquals(0, generate(token, maduraiRequirement).path("unpricedItems").size());

        // Copy the defaults into Coimbatore at +10%: Madurai's own rate is untouched, nothing is overwritten.
        MvcResult copied = mockMvc.perform(post("/api/rate-master/copy-district")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"toDistrict\": \"Coimbatore\", \"adjustPercent\": 10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.added").value(20))
                .andReturn();
        assertEquals(20, data(copied).path("added").asInt());
        MvcResult list = mockMvc.perform(get("/api/rate-master").header("Authorization", "Bearer " + token)).andReturn();
        BigDecimal coimbatoreCement = null;
        for (JsonNode item : data(list)) {
            if ("Coimbatore".equals(item.path("district").asText()) && "OPC 53 Grade Cement".equals(item.path("itemName").asText())) {
                coimbatoreCement = new BigDecimal(item.path("standardRate").asText());
            }
        }
        assertEquals(0, new BigDecimal("462.00").compareTo(coimbatoreCement), "420 + 10%");

        // Copying again adds nothing; copying from Madurai fills only what Coimbatore lacks (nothing).
        mockMvc.perform(post("/api/rate-master/copy-district")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"toDistrict\": \"Coimbatore\", \"adjustPercent\": 10}"))
                .andExpect(jsonPath("$.data.added").value(0))
                .andExpect(jsonPath("$.data.skipped").value(20));
        mockMvc.perform(post("/api/rate-master/copy-district")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"fromDistrict\": \"Madurai\", \"toDistrict\": \"Coimbatore\"}"))
                .andExpect(jsonPath("$.data.added").value(0));

        // Validation and tenant isolation.
        mockMvc.perform(post("/api/rate-master/copy-district")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"toDistrict\": \"Salem\", \"adjustPercent\": 300}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/rate-master/copy-district")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"toDistrict\": \"\"}"))
                .andExpect(status().isBadRequest());
        MvcResult otherList = mockMvc.perform(get("/api/rate-master").header("Authorization", "Bearer " + other)).andReturn();
        assertTrue(data(otherList).size() == 0);
    }

    private BigDecimal cementRate(JsonNode generated) {
        for (JsonNode item : generated.path("items")) {
            if ("CEMENT_RCC_STANDARD".equals(item.path("sourceRuleCode").asText())) {
                return new BigDecimal(item.path("rate").asText());
            }
        }
        throw new AssertionError("No cement line");
    }

    private JsonNode generate(String token, long id) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/house-requirements/" + id + "/generate-boq")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return data(result);
    }

    private long createRequirement(String token, String districtField) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/house-requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"label\": \"30x50\", " + districtField + " \"plotWidthFt\": 30, \"plotLengthFt\": 50, \"constructionGrade\": \"STANDARD\","
                                + " \"floors\": [{\"floorLevel\": 0, \"floorAreaSqft\": 1050, \"bedroomCount\": 2, \"bathroomCount\": 1, \"doorCount\": 6}]}"))
                .andExpect(status().isCreated())
                .andReturn();
        return data(result).path("id").asLong();
    }

    private JsonNode data(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String registerAndGetToken(String businessName, String email) throws Exception {
        RegisterRequest request = new RegisterRequest(businessName, "Owner", email, "SecurePass123");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return data(result).path("accessToken").asText();
    }
}
