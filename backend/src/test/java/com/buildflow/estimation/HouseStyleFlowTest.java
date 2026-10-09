package com.buildflow.estimation;

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
class HouseStyleFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void houseStyleIsSavedPerHouseValidatedAndScopedToTheBusiness() throws Exception {
        String token = register("Style Owner", "owner-style-a@buildflow.test");
        String other = register("Style Other", "owner-style-b@buildflow.test");
        long id = createRequirement(token);

        mockMvc.perform(get("/api/house-requirements/" + id + "/house-style").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exterior").doesNotExist());

        mockMvc.perform(put("/api/house-requirements/" + id + "/house-style")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"exterior\": \"#cfd8e8\", \"roof\": \"#555d66\", \"roofStyle\": \"FLAT\", \"rooms\": {\"r1\": \"#ffffff\"}}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/house-requirements/" + id + "/house-style").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.exterior").value("#cfd8e8"))
                .andExpect(jsonPath("$.data.roofStyle").value("FLAT"))
                .andExpect(jsonPath("$.data.rooms.r1").value("#ffffff"));

        mockMvc.perform(put("/api/house-requirements/" + id + "/house-style")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("[1, 2]"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/house-requirements/" + id + "/house-style")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"x\": \"" + "a".repeat(9000) + "\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/house-requirements/" + id + "/house-style").header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/house-requirements/" + id + "/house-style")
                        .header("Authorization", "Bearer " + other)
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    private long createRequirement(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/house-requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"label": "style", "plotWidthFt": 30, "plotLengthFt": 50, "constructionGrade": "STANDARD",
                                 "floors": [{"floorLevel": 0, "floorAreaSqft": 600, "bedroomCount": 1, "hasHall": true}]}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private String register(String business, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(business, "Owner", email, "SecurePass123"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("accessToken").asText();
    }
}
