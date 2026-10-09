package com.buildflow.dealer;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DealerQuoteFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String[][] RATES = {
            {"OPC 53 Grade Cement", "bag", "400"}, {"TMT Steel Fe500", "kg", "64"},
            {"Red Clay Brick 9x4x3", "nos", "8.5"}, {"River Sand", "cum", "1750"},
            {"Aggregate 20mm", "cum", "1500"}, {"Vitrified Tile", "sqft", "60"},
            {"Emulsion Paint", "ltr", "210"}, {"Flush Door", "nos", "3400"}
    };

    @Test
    void quoteRequestPrefillsFromRateCardsComparesDealersAndLocksAfterAward() throws Exception {
        String token = registerAndGetToken("Quote Builders", "owner-dealer-a@buildflow.test");
        long projectId = generateBoqProject(token);

        long fullDealer = createDealer(token, "Sri Murugan Traders", "Madurai", rateCardJson(RATES, 1.0));
        long cementOnly = createDealer(token, "Cement Depot", "Madurai",
                "[{\"itemName\": \"OPC 53 Grade Cement\", \"unit\": \"bag\", \"rate\": 410}]");

        MvcResult created = mockMvc.perform(post("/api/quote-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"projectId": %d, "title": "Main materials", "dealerIds": [%d, %d]}
                                """.formatted(projectId, fullDealer, cementOnly)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andReturn();
        JsonNode request = data(created);
        long requestId = request.path("id").asLong();

        // Cement appears on three BOQ lines (RCC, foundation, plaster) but is requested once, combined.
        assertEquals(8, request.path("items").size());
        assertEquals(0, new BigDecimal("1089").compareTo(new BigDecimal(
                item(request, "OPC 53 Grade Cement").path("quantity").asText())));

        JsonNode fullQuote = quoteOf(request, "Sri Murugan Traders");
        JsonNode partialQuote = quoteOf(request, "Cement Depot");
        // Rate-card prefill is indicative, never counted as a received quote.
        assertTrue(fullQuote.path("complete").asBoolean());
        assertEquals(1, partialQuote.path("pricedItemCount").asInt());
        assertTrue(!partialQuote.path("complete").asBoolean());
        assertEquals("INDICATIVE", fullQuote.path("lines").get(0).path("source").asText());
        assertTrue(request.path("lowestCompleteQuoteId").isNull());

        // 1,089 bags * 400 = 435,600 from the rate card.
        assertEquals(0, new BigDecimal("435600.00").compareTo(new BigDecimal(
                line(fullQuote, "OPC 53 Grade Cement").path("amount").asText())));

        // The full dealer confirms its rates with modest freight.
        put(token, requestId, fullQuote.path("id").asLong(), quoteBody(RATES, 1.0, 5000, 2000));
        // The cement-only dealer quotes everything slightly cheaper, but charges heavy delivery.
        MvcResult afterSecond = put(token, requestId, partialQuote.path("id").asLong(), quoteBody(RATES, 0.99, 60000, 0));
        JsonNode compared = data(afterSecond);

        JsonNode a = quoteOf(compared, "Sri Murugan Traders");
        JsonNode b = quoteOf(compared, "Cement Depot");
        assertTrue(new BigDecimal(b.path("materialTotal").asText()).compareTo(new BigDecimal(a.path("materialTotal").asText())) < 0,
                "Dealer B has the cheaper material total.");
        assertTrue(new BigDecimal(a.path("effectiveTotal").asText()).compareTo(new BigDecimal(b.path("effectiveTotal").asText())) < 0,
                "Delivery makes dealer A the cheaper effective total.");
        assertEquals(a.path("id").asLong(), compared.path("lowestCompleteQuoteId").asLong());

        mockMvc.perform(post("/api/quote-requests/" + requestId + "/award/" + a.path("id").asLong())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AWARDED"))
                .andExpect(jsonPath("$.data.awardedQuoteId").value(a.path("id").asLong()));

        // Once awarded, the request is locked.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/quote-requests/" + requestId + "/quotes/" + b.path("id").asLong())
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(quoteBody(RATES, 0.9, 0, 0)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dealersAreTenantScopedFilterableAndRequestsNeedABoq() throws Exception {
        String tokenA = registerAndGetToken("Dealer Owner A", "owner-dealer-b@buildflow.test");
        String tokenB = registerAndGetToken("Dealer Owner B", "owner-dealer-c@buildflow.test");

        long dealerId = createDealer(tokenA, "Kovai Sand Suppliers", "Coimbatore",
                "[{\"itemName\": \"River Sand\", \"unit\": \"cum\", \"rate\": 1700}]");
        createDealer(tokenA, "Madurai Bricks", "Madurai",
                "[{\"itemName\": \"Red Clay Brick 9x4x3\", \"unit\": \"nos\", \"rate\": 8}]");

        mockMvc.perform(get("/api/dealers").param("district", "coimbatore")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Kovai Sand Suppliers"));
        mockMvc.perform(get("/api/dealers").param("item", "brick")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Madurai Bricks"));

        mockMvc.perform(get("/api/dealers").header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.data.length()").value(0));
        mockMvc.perform(get("/api/dealers/" + dealerId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // A duplicate item on one rate card is rejected rather than silently shadowed.
        mockMvc.perform(post("/api/dealers")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "Dup", "rates": [
                                  {"itemName": "River Sand", "unit": "cum", "rate": 1},
                                  {"itemName": "river sand", "unit": "cum", "rate": 2}]}
                                """))
                .andExpect(status().isBadRequest());

        // A project with no BOQ has nothing to request quotes for.
        MvcResult project = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\": \"Empty\", \"contractValue\": 1000}"))
                .andExpect(status().isCreated())
                .andReturn();
        long emptyProjectId = data(project).path("id").asLong();
        mockMvc.perform(post("/api/quote-requests")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(APPLICATION_JSON)
                        .content("{\"projectId\": %d, \"title\": \"x\", \"dealerIds\": [%d]}".formatted(emptyProjectId, dealerId)))
                .andExpect(status().isBadRequest());
    }

    private JsonNode item(JsonNode request, String itemName) {
        for (JsonNode item : request.path("items")) {
            if (itemName.equals(item.path("itemName").asText())) {
                return item;
            }
        }
        throw new AssertionError("Item not found: " + itemName);
    }

    private JsonNode quoteOf(JsonNode request, String dealerName) {
        for (JsonNode quote : request.path("quotes")) {
            if (dealerName.equals(quote.path("dealerName").asText())) {
                return quote;
            }
        }
        throw new AssertionError("Quote not found for " + dealerName);
    }

    private JsonNode line(JsonNode quote, String itemName) {
        for (JsonNode line : quote.path("lines")) {
            if (itemName.equals(line.path("itemName").asText())) {
                return line;
            }
        }
        throw new AssertionError("Line not found: " + itemName);
    }

    private String rateCardJson(String[][] rates, double factor) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < rates.length; i++) {
            if (i > 0) builder.append(",");
            builder.append("{\"itemName\": \"%s\", \"unit\": \"%s\", \"rate\": %s}"
                    .formatted(rates[i][0], rates[i][1], new BigDecimal(rates[i][2]).multiply(BigDecimal.valueOf(factor))));
        }
        return builder.append("]").toString();
    }

    private String quoteBody(String[][] rates, double factor, int delivery, int loading) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < rates.length; i++) {
            if (i > 0) builder.append(",");
            builder.append("{\"itemName\": \"%s\", \"unitRate\": %s}"
                    .formatted(rates[i][0], new BigDecimal(rates[i][2]).multiply(BigDecimal.valueOf(factor))));
        }
        return "{\"status\": \"RECEIVED\", \"deliveryCharge\": %d, \"loadingCharge\": %d, \"lines\": [%s]}"
                .formatted(delivery, loading, builder);
    }

    private MvcResult put(String token, long requestId, long quoteId, String body) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/quote-requests/" + requestId + "/quotes/" + quoteId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
    }

    private long createDealer(String token, String name, String district, String ratesJson) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/dealers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "district": "%s", "phone": "9876543210", "rates": %s}
                                """.formatted(name, district, ratesJson)))
                .andExpect(status().isCreated())
                .andReturn();
        return data(result).path("id").asLong();
    }

    // Builds a project whose BOQ holds the generated estimate (cement appears on several lines).
    private long generateBoqProject(String token) throws Exception {
        for (String[] rate : RATES) {
            mockMvc.perform(post("/api/rate-master")
                            .header("Authorization", "Bearer " + token)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {"itemName": "%s", "category": "MATERIAL", "unit": "%s", "standardRate": %s}
                                    """.formatted(rate[0], rate[1], rate[2])))
                    .andExpect(status().isCreated());
        }
        MvcResult requirement = mockMvc.perform(post("/api/house-requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"label": "30x50", "plotWidthFt": 30, "plotLengthFt": 50, "constructionGrade": "STANDARD",
                                 "floors": [
                                   {"floorLevel": 0, "floorAreaSqft": 1050, "doorCount": 6},
                                   {"floorLevel": 1, "floorAreaSqft": 750, "doorCount": 4}]}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long requirementId = data(requirement).path("id").asLong();
        MvcResult generated = mockMvc.perform(post("/api/house-requirements/" + requirementId + "/generate-boq")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return data(generated).path("projectId").asLong();
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
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }
}
