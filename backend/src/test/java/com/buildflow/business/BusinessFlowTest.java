package com.buildflow.business;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BusinessFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void businessProfileCanBeReadImmediatelyAfterRegistrationAndUpdatedByAdmin() throws Exception {
        String token = registerAndGetToken("Riverside Builders", "owner-business-a@buildflow.test");

        // GET must work off the freshly-authenticated principal without a lazy-init failure.
        mockMvc.perform(get("/api/business")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Riverside Builders"));

        mockMvc.perform(put("/api/business")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "Riverside Builders Pvt Ltd", "gstin": "27AAAAA0000A1Z5", "stateName": "Maharashtra",
                                 "materialRegion": "Coimbatore, Tamil Nadu", "defaultGstRate": 12}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gstin").value("27AAAAA0000A1Z5"));

        mockMvc.perform(get("/api/business")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Riverside Builders Pvt Ltd"))
                .andExpect(jsonPath("$.data.stateName").value("Maharashtra"))
                .andExpect(jsonPath("$.data.materialRegion").value("Coimbatore, Tamil Nadu"))
                .andExpect(jsonPath("$.data.defaultGstRate").value(12.0));
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
