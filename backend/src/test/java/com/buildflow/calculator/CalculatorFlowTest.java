package com.buildflow.calculator;

import com.buildflow.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Expected values are worked by hand from the formulas listed in each result's assumptions. */
@SpringBootTest
@AutoConfigureMockMvc
class CalculatorFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void concreteVolumeMaterialsAndSteelFollowTheNominalMix() throws Exception {
        String token = registerAndGetToken("Calc Co", "owner-calc-a@buildflow.test");

        // 4 footings 1.5 x 1.5 x 0.4 m = 3.6 m3. M20 = 1:1.5:3. Dry = 3.6 x 1.54 = 5.544 m3.
        // Cement 5.544 / 5.5 = 1.008 m3 = 29.05 bags; sand 1.512; aggregate 3.024.
        // Steel 0.8% of 3.6 m3 = 226.08 kg.
        mockMvc.perform(post("/api/calculators/concrete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"grade": "M20", "members": [
                                  {"name": "Isolated footings", "type": "FOOTING", "count": 4, "lengthM": 1.5, "widthM": 1.5, "depthM": 0.4}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.concreteM3").value(3.6))
                .andExpect(jsonPath("$.data.mixRatio").value("1:1.5:3"))
                .andExpect(jsonPath("$.data.cementBags").value(29.05))
                .andExpect(jsonPath("$.data.sandM3").value(1.512))
                .andExpect(jsonPath("$.data.aggregateM3").value(3.024))
                .andExpect(jsonPath("$.data.steelKg").value(226.08))
                .andExpect(jsonPath("$.data.members[0].steelPercent").value(0.8))
                .andExpect(jsonPath("$.data.assumptions.length()").value(5));

        // Overriding steel % and adding 10% wastage: 1 column 0.3 x 0.3 x 3 m = 0.27 m3, steel 2% = 42.39 kg x 1.1.
        mockMvc.perform(post("/api/calculators/concrete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"grade": "M25", "wastagePercent": 10, "members": [
                                  {"name": "C1", "type": "COLUMN", "count": 1, "lengthM": 0.3, "widthM": 0.3, "depthM": 3, "steelPercent": 2}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.steelKg").value(46.63));
    }

    @Test
    void masonryCountsUnitsFromNetVolumeAndMortarFromTheRemainder() throws Exception {
        String token = registerAndGetToken("Calc Masonry", "owner-calc-b@buildflow.test");

        // 10 x 3 m wall less 3 sqm opening = 27 sqm net, 0.2 m thick = 5.4 m3 -> 5.4 / 0.002 = 2700 bricks.
        // Mortar wet = 5.4 - 2700 x 0.001539 = 1.2447 m3; dry x1.33 = 1.655; 1:6 -> cement 0.2365 m3 = 6.82 bags; sand 1.419.
        mockMvc.perform(post("/api/calculators/masonry")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"unitType": "RED_BRICK", "walls": [
                                  {"name": "Front wall", "lengthM": 10, "heightM": 3, "thicknessM": 0.2, "openingsSqm": 3}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.walls[0].netAreaSqm").value(27.0))
                .andExpect(jsonPath("$.data.walls[0].netVolumeM3").value(5.4))
                .andExpect(jsonPath("$.data.units").value(2700.0))
                .andExpect(jsonPath("$.data.cementBags").value(6.82))
                .andExpect(jsonPath("$.data.sandM3").value(1.419))
                .andExpect(jsonPath("$.data.mortarSandParts").value(6));
    }

    @Test
    void steelBarsUseDSquaredOver162AndCountStandardLengths() throws Exception {
        String token = registerAndGetToken("Calc Steel", "owner-calc-c@buildflow.test");

        // 10 x 12 mm bars of 6 m: 12^2/162.2 = 0.8878 kg/m x 60 m = 53.27 kg; 60 m / 12 m = 5 standard bars.
        // 8 mm stirrups: 100 x 1.2 m = 120 m x 0.3945 = 47.35 kg; 10 standard bars.
        mockMvc.perform(post("/api/calculators/steel")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"bars": [
                                  {"mark": "B1", "diameterMm": 12, "lengthM": 6, "nos": 10},
                                  {"mark": "S1", "diameterMm": 8, "lengthM": 1.2, "nos": 100}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bars[0].weightKg").value(53.27))
                .andExpect(jsonPath("$.data.byDiameter[0].diameterMm").value(8))
                .andExpect(jsonPath("$.data.byDiameter[0].standardBarsRequired").value(10))
                .andExpect(jsonPath("$.data.byDiameter[1].standardBarsRequired").value(5))
                .andExpect(jsonPath("$.data.totalKg").value(100.62));

        mockMvc.perform(post("/api/calculators/steel")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"bars\": [{\"mark\": \"X\", \"diameterMm\": 3, \"lengthM\": 1, \"nos\": 1}]}"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(post("/api/calculators/concrete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"grade\": \"M20\", \"members\": []}"))
                .andExpect(status().isUnprocessableEntity());
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
