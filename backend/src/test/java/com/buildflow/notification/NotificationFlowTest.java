package com.buildflow.notification;

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
class NotificationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void realBusinessEventsGenerateNotificationsVisibleOnlyToTheOwningBusiness() throws Exception {
        String tokenA = registerAndGetToken("Green Villa Constructions", "owner-notif-a@buildflow.test");
        String tokenB = registerAndGetToken("Skyline Commercial", "owner-notif-b@buildflow.test");

        long projectId = createProject(tokenA, "Green Villa", "1000000", "100000");

        // Submitting a daily report should notify ADMIN/PROJECT_MANAGER (the registering user is ADMIN).
        mockMvc.perform(post("/api/projects/" + projectId + "/daily-reports")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"reportDate": "2026-09-19", "workersPresent": 5}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("DAILY_REPORT"))
                .andExpect(jsonPath("$.data.content[0].read").value(false));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        // Business B must never see business A's notifications.
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));

        // An expense that pushes actual cost past the estimated cost should trigger a budget-variance alert.
        mockMvc.perform(post("/api/projects/" + projectId + "/expenses")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"category": "MATERIAL", "amount": 150000, "date": "2026-09-19"}
                                """))
                .andExpect(status().isCreated());

        MvcResult listResult = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("BUDGET_VARIANCE"))
                .andReturn();

        long notificationId = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .path("data").path("content").get(0).path("id").asLong();

        mockMvc.perform(patch("/api/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));
    }

    private long createProject(String token, String name, String contractValue, String estimatedCost) throws Exception {
        String payload = """
                {"name": "%s", "contractValue": %s, "estimatedCost": %s}
                """.formatted(name, contractValue, estimatedCost);

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
