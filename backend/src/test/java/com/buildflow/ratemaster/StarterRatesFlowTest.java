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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StarterRatesFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void starterRatesFillOnlyMissingItemsAndLetAnEstimateRunImmediately() throws Exception {
        String token = registerAndGetToken("Starter Builders", "owner-starter-a@buildflow.test");
        String other = registerAndGetToken("Starter Others", "owner-starter-b@buildflow.test");

        // An existing rate (different casing) must be kept as the user set it, not duplicated.
        mockMvc.perform(post("/api/rate-master")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"itemName\": \"opc 53 grade cement\", \"category\": \"MATERIAL\", \"unit\": \"bag\", \"standardRate\": 999}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/rate-master/starter").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.added").value(19))
                .andExpect(jsonPath("$.data.alreadyPresent").value(1));
        mockMvc.perform(get("/api/rate-master").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.length()").value(20));

        // Running it again adds nothing.
        mockMvc.perform(post("/api/rate-master/starter").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.added").value(0));

        // Another business is unaffected.
        mockMvc.perform(get("/api/rate-master").header("Authorization", "Bearer " + other))
                .andExpect(jsonPath("$.data.length()").value(0));

        // With starter rates an estimate prices every line with no further setup.
        MvcResult requirement = mockMvc.perform(post("/api/house-requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"label": "30x50", "plotWidthFt": 30, "plotLengthFt": 50, "constructionGrade": "STANDARD",
                                 "floors": [{"floorLevel": 0, "floorAreaSqft": 1050, "bedroomCount": 2, "bathroomCount": 1,
                                             "hasKitchen": true, "hasHall": true, "doorCount": 6}]}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long id = data(requirement).path("id").asLong();
        MvcResult generated = mockMvc.perform(post("/api/house-requirements/" + id + "/generate-boq")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode result = data(generated);
        assertEquals(0, result.path("unpricedItems").size());
        // Includes labour and the electrical/plumbing lines, not only the core materials.
        boolean hasLabour = false;
        for (JsonNode item : result.path("items")) {
            if ("LABOUR".equals(item.path("category").asText())) hasLabour = true;
        }
        assertEquals(true, hasLabour);
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
