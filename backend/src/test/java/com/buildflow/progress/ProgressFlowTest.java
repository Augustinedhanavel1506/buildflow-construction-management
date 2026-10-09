package com.buildflow.progress;

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
class ProgressFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void progressStagesRollUpIntoProjectOverallProgress() throws Exception {
        String tokenA = registerAndGetToken("Green Villa Constructions", "owner-prog-a@buildflow.test");
        String tokenB = registerAndGetToken("Skyline Commercial", "owner-prog-b@buildflow.test");

        long projectId = createProject(tokenA, "Green Villa");

        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overallProgress").value(0));

        long foundationId = createStage(tokenA, projectId, "Foundation", 100);
        createStage(tokenA, projectId, "Structure", 50);

        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overallProgress").value(75));

        mockMvc.perform(patch("/api/progress/" + foundationId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"percentComplete": 80}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/projects/" + projectId + "/progress")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    private long createStage(String token, long projectId, String stageName, int percent) throws Exception {
        String payload = """
                {"stageName": "%s", "percentComplete": %d}
                """.formatted(stageName, percent);

        MvcResult result = mockMvc.perform(post("/api/projects/" + projectId + "/progress")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
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
