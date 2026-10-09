package com.buildflow.review;

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
class EngineerReviewFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void engineerReviewsValidatesAndIsConfinedToReviewAreas() throws Exception {
        String admin = registerAndGetToken("Review Builders", "owner-review-a@buildflow.test");
        long[] estimate = createEstimate(admin);
        long projectId = estimate[1];

        long engineerId = createEngineer(admin, "Asha Rao", "asha.review@buildflow.test", "KA-1234");
        String engineer = login("asha.review@buildflow.test");

        // An engineer is confined to reviews, calculators and notifications.
        for (String path : new String[]{"/api/projects", "/api/rate-master", "/api/dealers", "/api/house-requirements"}) {
            mockMvc.perform(get(path).header("Authorization", "Bearer " + engineer)).andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/team/engineers").header("Authorization", "Bearer " + engineer)).andExpect(status().isForbidden());

        // Only the owner side can request a review, and only one at a time per project.
        mockMvc.perform(post("/api/review-requests")
                        .header("Authorization", "Bearer " + engineer)
                        .contentType(APPLICATION_JSON)
                        .content("{\"projectId\": %d, \"engineerId\": %d}".formatted(projectId, engineerId)))
                .andExpect(status().isForbidden());
        long reviewId = requestReview(admin, projectId, engineerId, "Please check the slab quantities");
        mockMvc.perform(post("/api/review-requests")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(APPLICATION_JSON)
                        .content("{\"projectId\": %d, \"engineerId\": %d}".formatted(projectId, engineerId)))
                .andExpect(status().isBadRequest());

        // The engineer is notified personally; the owner is not.
        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + engineer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + engineer))
                .andExpect(jsonPath("$.data.content[0].type").value("REVIEW_REQUEST"));

        MvcResult listed = mockMvc.perform(get("/api/review-requests").header("Authorization", "Bearer " + engineer))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(1, data(listed).size());

        MvcResult detail = mockMvc.perform(get("/api/review-requests/" + reviewId).header("Authorization", "Bearer " + engineer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plan.floors.length()").value(2))
                .andExpect(jsonPath("$.data.floorPlans.floors.length()").value(2))
                .andReturn();
        JsonNode items = data(detail).path("items");
        long cementId = lineBySourceRule(items, "CEMENT_RCC_STANDARD").path("id").asLong();

        // The owner side cannot submit a review.
        mockMvc.perform(post("/api/review-requests/" + reviewId + "/submit")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(APPLICATION_JSON)
                        .content("{\"outcome\": \"COMPLETE\", \"approveRemaining\": true}"))
                .andExpect(status().isForbidden());

        // Sending back without saying why is rejected.
        mockMvc.perform(post("/api/review-requests/" + reviewId + "/submit")
                        .header("Authorization", "Bearer " + engineer)
                        .contentType(APPLICATION_JSON)
                        .content("{\"outcome\": \"REQUEST_CHANGES\"}"))
                .andExpect(status().isBadRequest());

        // Correct cement to 800 bags and approve everything else as estimated.
        mockMvc.perform(post("/api/review-requests/" + reviewId + "/submit")
                        .header("Authorization", "Bearer " + engineer)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"outcome": "COMPLETE", "overallNote": "Reviewed against drawings", "approveRemaining": true,
                                 "lines": [{"boqItemId": %d, "quantity": 800, "comment": "Heavier slab per S-02"}]}
                                """.formatted(cementId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        MvcResult boq = mockMvc.perform(get("/api/projects/" + projectId + "/boq").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode item : data(boq)) {
            assertEquals("ENGINEER_VALIDATED", item.path("estimateSource").asText());
            assertEquals("Asha Rao, Reg. KA-1234", item.path("validatedBy").asText());
        }
        JsonNode cement = lineBySourceRule(data(boq), "CEMENT_RCC_STANDARD");
        assertEquals(0, new BigDecimal("800").compareTo(new BigDecimal(cement.path("quantity").asText())));
        assertEquals("Heavier slab per S-02", cement.path("validationNote").asText());

        // The decision is recorded, and the owner is notified.
        mockMvc.perform(get("/api/review-requests/" + reviewId).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.data.decisions.length()").value(1))
                .andExpect(jsonPath("$.data.decisions[0].correctedQuantity").value(800));
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.data.content[0].type").value("REVIEW_COMPLETED"));

        // A completed review cannot be submitted twice.
        mockMvc.perform(post("/api/review-requests/" + reviewId + "/submit")
                        .header("Authorization", "Bearer " + engineer)
                        .contentType(APPLICATION_JSON)
                        .content("{\"outcome\": \"COMPLETE\", \"approveRemaining\": true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changesRequestedLeavesTheBoqUntouchedAndReviewsAreTenantScoped() throws Exception {
        String admin = registerAndGetToken("Review Owners", "owner-review-b@buildflow.test");
        String otherAdmin = registerAndGetToken("Review Other", "owner-review-c@buildflow.test");
        long[] estimate = createEstimate(admin);
        long projectId = estimate[1];

        long engineerId = createEngineer(admin, "Ravi Kumar", "ravi.review@buildflow.test", null);
        createEngineer(otherAdmin, "Other Engineer", "other.review@buildflow.test", null);
        String engineer = login("ravi.review@buildflow.test");
        String otherEngineer = login("other.review@buildflow.test");

        long reviewId = requestReview(admin, projectId, engineerId, null);

        // Another business's engineer, or admin, cannot see this review.
        mockMvc.perform(get("/api/review-requests/" + reviewId).header("Authorization", "Bearer " + otherEngineer))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/review-requests/" + reviewId).header("Authorization", "Bearer " + otherAdmin))
                .andExpect(status().isNotFound());
        // An engineer cannot be assigned from another business.
        mockMvc.perform(post("/api/review-requests")
                        .header("Authorization", "Bearer " + otherAdmin)
                        .contentType(APPLICATION_JSON)
                        .content("{\"projectId\": %d, \"engineerId\": %d}".formatted(projectId, engineerId)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/review-requests/" + reviewId + "/submit")
                        .header("Authorization", "Bearer " + engineer)
                        .contentType(APPLICATION_JSON)
                        .content("{\"outcome\": \"REQUEST_CHANGES\", \"overallNote\": \"Foundation depth missing\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CHANGES_REQUESTED"));

        MvcResult boq = mockMvc.perform(get("/api/projects/" + projectId + "/boq").header("Authorization", "Bearer " + admin))
                .andReturn();
        for (JsonNode item : data(boq)) {
            assertEquals("SYSTEM_PRELIMINARY", item.path("estimateSource").asText());
        }
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.data.content[0].title").value("Changes requested: 30x50"));

        // After changes are requested a new review can be raised, and the owner can cancel it.
        long second = requestReview(admin, projectId, engineerId, "Updated the plan");
        mockMvc.perform(post("/api/review-requests/" + second + "/cancel").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // Engineer accounts need a real password and a unique email.
        mockMvc.perform(post("/api/team/engineers")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(APPLICATION_JSON)
                        .content("{\"fullName\": \"X\", \"email\": \"ravi.review@buildflow.test\", \"password\": \"SecurePass123\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/team/engineers")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(APPLICATION_JSON)
                        .content("{\"fullName\": \"X\", \"email\": \"short.review@buildflow.test\", \"password\": \"123\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    private JsonNode lineBySourceRule(JsonNode items, String ruleCode) {
        for (JsonNode item : items) {
            if (ruleCode.equals(item.path("sourceRuleCode").asText())) {
                return item;
            }
        }
        throw new AssertionError("No line for " + ruleCode);
    }

    private long requestReview(String token, long projectId, long engineerId, String message) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/review-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(message == null
                                ? "{\"projectId\": %d, \"engineerId\": %d}".formatted(projectId, engineerId)
                                : "{\"projectId\": %d, \"engineerId\": %d, \"message\": \"%s\"}".formatted(projectId, engineerId, message)))
                .andExpect(status().isCreated())
                .andReturn();
        return data(result).path("id").asLong();
    }

    private long createEngineer(String token, String name, String email, String registration) throws Exception {
        String body = registration == null
                ? "{\"fullName\": \"%s\", \"email\": \"%s\", \"password\": \"SecurePass123\"}".formatted(name, email)
                : "{\"fullName\": \"%s\", \"email\": \"%s\", \"password\": \"SecurePass123\", \"registrationNo\": \"%s\"}".formatted(name, email, registration);
        MvcResult result = mockMvc.perform(post("/api/team/engineers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return data(result).path("id").asLong();
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"SecurePass123\"}".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.role").value("ENGINEER"))
                .andReturn();
        return data(result).path("accessToken").asText();
    }

    // Returns {houseRequirementId, projectId} with a generated BOQ.
    private long[] createEstimate(String token) throws Exception {
        String[][] rates = {{"OPC 53 Grade Cement", "bag", "420"}, {"TMT Steel Fe500", "kg", "65"},
                {"Red Clay Brick 9x4x3", "nos", "9"}, {"River Sand", "cum", "1800"},
                {"Aggregate 20mm", "cum", "1600"}, {"Vitrified Tile", "sqft", "65"},
                {"Emulsion Paint", "ltr", "220"}, {"Flush Door", "nos", "3500"}};
        for (String[] rate : rates) {
            mockMvc.perform(post("/api/rate-master")
                            .header("Authorization", "Bearer " + token)
                            .contentType(APPLICATION_JSON)
                            .content("{\"itemName\": \"%s\", \"category\": \"MATERIAL\", \"unit\": \"%s\", \"standardRate\": %s}"
                                    .formatted(rate[0], rate[1], rate[2])))
                    .andExpect(status().isCreated());
        }
        MvcResult requirement = mockMvc.perform(post("/api/house-requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"label": "30x50", "plotWidthFt": 30, "plotLengthFt": 50, "constructionGrade": "STANDARD",
                                 "floors": [{"floorLevel": 0, "floorAreaSqft": 1050, "doorCount": 6},
                                            {"floorLevel": 1, "floorAreaSqft": 750, "doorCount": 4}]}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long requirementId = data(requirement).path("id").asLong();
        MvcResult generated = mockMvc.perform(post("/api/house-requirements/" + requirementId + "/generate-boq")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(data(generated).path("items").size() > 0);
        return new long[]{requirementId, data(generated).path("projectId").asLong()};
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
