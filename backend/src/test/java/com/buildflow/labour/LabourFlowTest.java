package com.buildflow.labour;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LabourFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void attendanceSubmissionIsIdempotentAndDrivesLabourCost() throws Exception {
        String token = registerAndGetToken("Green Villa Constructions", "owner-lab-a@buildflow.test");
        long projectId = createProject(token, "Green Villa");

        long masonId = createWorker(token, projectId, "Ramesh", "Mason", "900");
        long helperId = createWorker(token, projectId, "Suresh", "Helper", "650");

        String attendancePayload = """
                {"date": "2026-09-17", "entries": [
                  {"workerId": %d, "present": true},
                  {"workerId": %d, "present": true}
                ]}
                """.formatted(masonId, helperId);

        mockMvc.perform(post("/api/projects/" + projectId + "/labour/attendance")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(attendancePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].present").value(true))
                .andExpect(jsonPath("$.data[1].present").value(true));

        mockMvc.perform(get("/api/projects/" + projectId + "/labour/cost")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCost").value(1550.0));

        String resubmitPayload = """
                {"date": "2026-09-17", "entries": [
                  {"workerId": %d, "present": false}
                ]}
                """.formatted(helperId);

        mockMvc.perform(post("/api/projects/" + projectId + "/labour/attendance")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(resubmitPayload))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/projects/" + projectId + "/labour/cost")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCost").value(900.0));

        mockMvc.perform(get("/api/projects/" + projectId + "/labour/attendance")
                        .header("Authorization", "Bearer " + token)
                        .param("date", "2026-09-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void workerRosterEnforcesTenantIsolation() throws Exception {
        String tokenA = registerAndGetToken("Green Villa Constructions", "owner-lab-b@buildflow.test");
        String tokenB = registerAndGetToken("Skyline Commercial", "owner-lab-c@buildflow.test");

        long projectId = createProject(tokenA, "Green Villa");
        createWorker(tokenA, projectId, "Ramesh", "Mason", "900");

        mockMvc.perform(get("/api/projects/" + projectId + "/labour/workers")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    private long createWorker(String token, long projectId, String name, String role, String dailyRate) throws Exception {
        String payload = """
                {"name": "%s", "role": "%s", "dailyRate": %s}
                """.formatted(name, role, dailyRate);

        MvcResult result = mockMvc.perform(post("/api/projects/" + projectId + "/labour/workers")
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
