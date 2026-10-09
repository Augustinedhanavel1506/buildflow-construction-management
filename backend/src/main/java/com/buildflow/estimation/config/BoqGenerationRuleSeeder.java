package com.buildflow.estimation.config;

import com.buildflow.boq.entity.BoqCategory;
import com.buildflow.estimation.entity.BoqComponent;
import com.buildflow.estimation.entity.BoqGenerationRule;
import com.buildflow.estimation.entity.ConfidenceLevel;
import com.buildflow.estimation.entity.ConstructionGrade;
import com.buildflow.estimation.entity.EstimationBasis;
import com.buildflow.estimation.entity.RuleSourceType;
import com.buildflow.estimation.repository.BoqGenerationRuleRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Seeds boq_generation_rules with placeholder coefficients on first boot, only if the table is
 * empty. Every row here is sourceType=PLACEHOLDER, verified=false, confidence=LOW — these are
 * illustrative starting numbers, NOT sourced from a schedule of rates yet. Replace them (or add
 * business-specific overrides) once real TN PWD SOR / CPWD DSR figures, or completed-project
 * averages, are available — a non-null business_id row takes precedence over these platform
 * defaults for the same ruleCode (see BoqGenerationRuleRepository / BoqGenerationService).
 */
@Component
public class BoqGenerationRuleSeeder implements ApplicationRunner {

    private static final BigDecimal ECONOMY_MULTIPLIER = new BigDecimal("0.92");
    private static final BigDecimal PREMIUM_MULTIPLIER = new BigDecimal("1.12");

    private final BoqGenerationRuleRepository ruleRepository;

    public BoqGenerationRuleSeeder(BoqGenerationRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Insert only rule codes that don't exist yet, so adding templates later reaches databases
        // that were seeded earlier, without touching rules an admin has since edited.
        for (Template template : TEMPLATES) {
            saveIfMissing(buildRule(template, ConstructionGrade.ECONOMY, ECONOMY_MULTIPLIER));
            saveIfMissing(buildRule(template, ConstructionGrade.STANDARD, BigDecimal.ONE));
            saveIfMissing(buildRule(template, ConstructionGrade.PREMIUM, PREMIUM_MULTIPLIER));
        }
    }

    private void saveIfMissing(BoqGenerationRule rule) {
        if (ruleRepository.findByRuleCode(rule.getRuleCode()).isEmpty()) {
            ruleRepository.save(rule);
        }
    }

    private BoqGenerationRule buildRule(Template t, ConstructionGrade grade, BigDecimal multiplier) {
        BoqGenerationRule rule = new BoqGenerationRule();
        rule.setRuleCode(t.code() + "_" + grade.name());
        rule.setBaseRuleCode(rule.getRuleCode());
        rule.setComponent(t.component());
        rule.setConstructionGrade(grade);
        rule.setItemName(t.itemName());
        rule.setBoqCategory(t.boqCategory());
        rule.setUnit(t.unit());
        rule.setBasis(t.basis());
        // Frame quantities do not depend on the finish level, so the grade multiplier does not apply.
        BigDecimal factor = isStructural(t.basis()) ? BigDecimal.ONE : multiplier;
        rule.setCoefficient(scale(t.coefficient().multiply(factor)));
        rule.setWastagePercent(t.wastagePercent());
        if (t.minCoefficient() != null) {
            rule.setMinCoefficient(scale(t.minCoefficient().multiply(factor)));
            rule.setMaxCoefficient(scale(t.maxCoefficient().multiply(factor)));
        }
        rule.setApplicableFloorMin(t.applicableFloorMin());
        rule.setApplicableFloorMax(t.applicableFloorMax());
        rule.setSourceType(RuleSourceType.PLACEHOLDER);
        rule.setSourceReference(
                "Illustrative starting value, not yet sourced — pending calibration against "
                        + "TN PWD Schedule of Rates 2025-26 / CPWD DSR coefficients or completed-project averages.");
        rule.setConfidenceLevel(ConfidenceLevel.LOW);
        rule.setVerified(false);
        rule.setVersion(1);
        rule.setEffectiveFrom(LocalDate.now());
        rule.setActive(true);
        return rule;
    }

    private boolean isStructural(EstimationBasis basis) {
        return basis == EstimationBasis.STRUCTURAL_CONCRETE_M3 || basis == EstimationBasis.STRUCTURAL_STEEL_KG;
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private record Template(
            String code, BoqComponent component, String itemName, BoqCategory boqCategory, String unit,
            EstimationBasis basis, BigDecimal coefficient, BigDecimal wastagePercent,
            BigDecimal minCoefficient, BigDecimal maxCoefficient,
            Integer applicableFloorMin, Integer applicableFloorMax
    ) {
    }

    private static final List<Template> TEMPLATES = List.of(
            new Template("CEMENT_RCC", BoqComponent.RCC_FRAMING, "OPC 53 Grade Cement", BoqCategory.MATERIAL, "bag",
                    EstimationBasis.TOTAL_BUILTUP_AREA, new BigDecimal("0.40"), new BigDecimal("3.00"),
                    new BigDecimal("0.36"), new BigDecimal("0.44"), null, null),
            new Template("CEMENT_FOUNDATION", BoqComponent.FOUNDATION, "OPC 53 Grade Cement", BoqCategory.MATERIAL, "bag",
                    EstimationBasis.GROUND_FLOOR_AREA, new BigDecimal("0.08"), new BigDecimal("3.00"),
                    new BigDecimal("0.07"), new BigDecimal("0.09"), null, null),
            new Template("STEEL_RCC", BoqComponent.RCC_FRAMING, "TMT Steel Fe500", BoqCategory.MATERIAL, "kg",
                    EstimationBasis.TOTAL_BUILTUP_AREA, new BigDecimal("3.5"), new BigDecimal("3.00"),
                    new BigDecimal("3.2"), new BigDecimal("3.8"), null, null),
            new Template("BRICK_MASONRY", BoqComponent.MASONRY, "Red Clay Brick 9x4x3", BoqCategory.MATERIAL, "nos",
                    EstimationBasis.TOTAL_BUILTUP_AREA, new BigDecimal("9.5"), new BigDecimal("5.00"),
                    new BigDecimal("8.8"), new BigDecimal("10.2"), null, null),
            new Template("SAND_MASONRY", BoqComponent.MASONRY, "River Sand", BoqCategory.MATERIAL, "cum",
                    EstimationBasis.TOTAL_BUILTUP_AREA, new BigDecimal("0.035"), new BigDecimal("5.00"),
                    new BigDecimal("0.03"), new BigDecimal("0.04"), null, null),
            new Template("AGGREGATE_RCC", BoqComponent.RCC_FRAMING, "Aggregate 20mm", BoqCategory.MATERIAL, "cum",
                    EstimationBasis.TOTAL_BUILTUP_AREA, new BigDecimal("0.028"), new BigDecimal("3.00"),
                    new BigDecimal("0.025"), new BigDecimal("0.032"), null, null),
            new Template("TILE_FLOORING", BoqComponent.FLOORING, "Vitrified Tile", BoqCategory.MATERIAL, "sqft",
                    EstimationBasis.TOTAL_BUILTUP_AREA, new BigDecimal("1.05"), new BigDecimal("8.00"),
                    null, null, null, null),
            new Template("PAINT_EMULSION", BoqComponent.PAINTING, "Emulsion Paint", BoqCategory.MATERIAL, "ltr",
                    EstimationBasis.TOTAL_BUILTUP_AREA, new BigDecimal("0.12"), new BigDecimal("5.00"),
                    null, null, null, null),
            new Template("DOOR_FLUSH", BoqComponent.DOORS_WINDOWS, "Flush Door", BoqCategory.MATERIAL, "nos",
                    EstimationBasis.DOOR_COUNT, BigDecimal.ONE, BigDecimal.ZERO,
                    null, null, null, null),

            // Plastering reuses the cement and sand items above, so no extra rates are needed for it.
            new Template("CEMENT_PLASTER", BoqComponent.PLASTERING, "OPC 53 Grade Cement", BoqCategory.MATERIAL, "bag",
                    EstimationBasis.TOTAL_BUILTUP_AREA, new BigDecimal("0.14"), new BigDecimal("3.00"),
                    new BigDecimal("0.12"), new BigDecimal("0.16"), null, null),
            new Template("SAND_PLASTER", BoqComponent.PLASTERING, "River Sand", BoqCategory.MATERIAL, "cum",
                    EstimationBasis.TOTAL_BUILTUP_AREA, new BigDecimal("0.012"), new BigDecimal("5.00"),
                    new BigDecimal("0.010"), new BigDecimal("0.014"), null, null),

            new Template("WATERPROOF_TERRACE", BoqComponent.WATERPROOFING, "Waterproofing Chemical", BoqCategory.MATERIAL, "kg",
                    EstimationBasis.GROUND_FLOOR_AREA, new BigDecimal("0.06"), new BigDecimal("5.00"),
                    null, null, null, null),
            new Template("WATERPROOF_BATHROOM", BoqComponent.WATERPROOFING, "Waterproofing Chemical", BoqCategory.MATERIAL, "kg",
                    EstimationBasis.BATHROOM_COUNT, new BigDecimal("4.0"), new BigDecimal("5.00"),
                    null, null, null, null),

            new Template("WIRE_ELECTRICAL", BoqComponent.ELECTRICAL, "Copper Wire", BoqCategory.MATERIAL, "m",
                    EstimationBasis.TOTAL_BUILTUP_AREA, new BigDecimal("4.5"), new BigDecimal("5.00"),
                    new BigDecimal("3.8"), new BigDecimal("5.2"), null, null),
            new Template("CONDUIT_ELECTRICAL", BoqComponent.ELECTRICAL, "Electrical Conduit Pipe", BoqCategory.MATERIAL, "m",
                    EstimationBasis.TOTAL_BUILTUP_AREA, new BigDecimal("2.5"), new BigDecimal("5.00"),
                    null, null, null, null),
            new Template("SWITCHES_ELECTRICAL", BoqComponent.ELECTRICAL, "Switch and Socket", BoqCategory.MATERIAL, "nos",
                    EstimationBasis.ROOM_COUNT, new BigDecimal("8"), new BigDecimal("0.00"),
                    null, null, null, null),

            new Template("PIPE_PLUMBING", BoqComponent.PLUMBING, "Water Supply Pipe", BoqCategory.MATERIAL, "m",
                    EstimationBasis.TOTAL_BUILTUP_AREA, new BigDecimal("1.2"), new BigDecimal("5.00"),
                    null, null, null, null),
            new Template("FITTINGS_PLUMBING", BoqComponent.PLUMBING, "Sanitary Fittings Set", BoqCategory.MATERIAL, "nos",
                    EstimationBasis.BATHROOM_COUNT, BigDecimal.ONE, BigDecimal.ZERO,
                    null, null, null, null),

            // Labour is estimated in working days per sq.ft; wage rates come from the business's rate master.
            new Template("LABOUR_MASON", BoqComponent.MASONRY, "Mason Labour", BoqCategory.LABOUR, "day",
                    EstimationBasis.TOTAL_BUILTUP_AREA, new BigDecimal("0.20"), new BigDecimal("0.00"),
                    new BigDecimal("0.16"), new BigDecimal("0.25"), null, null),
            new Template("LABOUR_HELPER", BoqComponent.MISC, "Helper Labour", BoqCategory.LABOUR, "day",
                    EstimationBasis.TOTAL_BUILTUP_AREA, new BigDecimal("0.35"), new BigDecimal("0.00"),
                    new BigDecimal("0.28"), new BigDecimal("0.42"), null, null),
            new Template("LABOUR_BARBENDER", BoqComponent.RCC_FRAMING, "Bar Bender Labour", BoqCategory.LABOUR, "day",
                    EstimationBasis.TOTAL_BUILTUP_AREA, new BigDecimal("0.06"), new BigDecimal("0.00"),
                    null, null, null, null),
            new Template("LABOUR_ELECTRICIAN", BoqComponent.ELECTRICAL, "Electrician Labour", BoqCategory.LABOUR, "day",
                    EstimationBasis.TOTAL_BUILTUP_AREA, new BigDecimal("0.02"), new BigDecimal("0.00"),
                    null, null, null, null),
            new Template("LABOUR_PLUMBER", BoqComponent.PLUMBING, "Plumber Labour", BoqCategory.LABOUR, "day",
                    EstimationBasis.TOTAL_BUILTUP_AREA, new BigDecimal("0.02"), new BigDecimal("0.00"),
                    null, null, null, null),
            new Template("LABOUR_PAINTER", BoqComponent.PAINTING, "Painter Labour", BoqCategory.LABOUR, "day",
                    EstimationBasis.TOTAL_BUILTUP_AREA, new BigDecimal("0.04"), new BigDecimal("0.00"),
                    null, null, null, null),

            // Rules that apply only when every floor has a drawn plan. They replace the built-up-area
            // thumb rules for walls, plaster, paint and flooring, using real wall and room areas.
            // Per sq.ft of wall: 9" external wall ~9.3 bricks, 4.5" internal wall ~4.65.
            new Template("BRICK_EXT_DRAWN", BoqComponent.MASONRY, "Red Clay Brick 9x4x3", BoqCategory.MATERIAL, "nos",
                    EstimationBasis.EXTERNAL_WALL_AREA, new BigDecimal("9.3"), new BigDecimal("5.00"),
                    new BigDecimal("8.8"), new BigDecimal("9.8"), null, null),
            new Template("BRICK_INT_DRAWN", BoqComponent.MASONRY, "Red Clay Brick 9x4x3", BoqCategory.MATERIAL, "nos",
                    EstimationBasis.INTERNAL_WALL_AREA, new BigDecimal("4.65"), new BigDecimal("5.00"),
                    new BigDecimal("4.4"), new BigDecimal("4.9"), null, null),
            new Template("CEMENT_MORTAR_EXT_DRAWN", BoqComponent.MASONRY, "OPC 53 Grade Cement", BoqCategory.MATERIAL, "bag",
                    EstimationBasis.EXTERNAL_WALL_AREA, new BigDecimal("0.027"), new BigDecimal("3.00"),
                    null, null, null, null),
            new Template("CEMENT_MORTAR_INT_DRAWN", BoqComponent.MASONRY, "OPC 53 Grade Cement", BoqCategory.MATERIAL, "bag",
                    EstimationBasis.INTERNAL_WALL_AREA, new BigDecimal("0.0135"), new BigDecimal("3.00"),
                    null, null, null, null),
            new Template("SAND_MORTAR_EXT_DRAWN", BoqComponent.MASONRY, "River Sand", BoqCategory.MATERIAL, "cum",
                    EstimationBasis.EXTERNAL_WALL_AREA, new BigDecimal("0.0056"), new BigDecimal("5.00"),
                    null, null, null, null),
            new Template("SAND_MORTAR_INT_DRAWN", BoqComponent.MASONRY, "River Sand", BoqCategory.MATERIAL, "cum",
                    EstimationBasis.INTERNAL_WALL_AREA, new BigDecimal("0.0028"), new BigDecimal("5.00"),
                    null, null, null, null),
            // 12 mm plaster in 1:4 mortar: about 0.0082 bags of cement and 0.0012 cum of sand per sq.ft.
            new Template("CEMENT_PLASTER_DRAWN", BoqComponent.PLASTERING, "OPC 53 Grade Cement", BoqCategory.MATERIAL, "bag",
                    EstimationBasis.PLASTER_AREA, new BigDecimal("0.0082"), new BigDecimal("3.00"),
                    new BigDecimal("0.0075"), new BigDecimal("0.0090"), null, null),
            new Template("SAND_PLASTER_DRAWN", BoqComponent.PLASTERING, "River Sand", BoqCategory.MATERIAL, "cum",
                    EstimationBasis.PLASTER_AREA, new BigDecimal("0.0012"), new BigDecimal("5.00"),
                    null, null, null, null),
            // Two coats at roughly 120 sq.ft per litre per coat.
            new Template("PAINT_DRAWN", BoqComponent.PAINTING, "Emulsion Paint", BoqCategory.MATERIAL, "ltr",
                    EstimationBasis.PLASTER_AREA, new BigDecimal("0.0167"), new BigDecimal("5.00"),
                    null, null, null, null),
            // RCC frame from drawn columns, beams, slabs and footings, in M20 (1:1.5:3): per m3 of
            // concrete, 1.54 dry volume gives 8.07 bags of cement, 0.42 m3 sand and 0.84 m3 aggregate.
            new Template("CEMENT_STRUCT", BoqComponent.RCC_FRAMING, "OPC 53 Grade Cement", BoqCategory.MATERIAL, "bag",
                    EstimationBasis.STRUCTURAL_CONCRETE_M3, new BigDecimal("8.07"), new BigDecimal("3.00"),
                    new BigDecimal("7.5"), new BigDecimal("8.6"), null, null),
            new Template("SAND_STRUCT", BoqComponent.RCC_FRAMING, "River Sand", BoqCategory.MATERIAL, "cum",
                    EstimationBasis.STRUCTURAL_CONCRETE_M3, new BigDecimal("0.42"), new BigDecimal("5.00"),
                    null, null, null, null),
            new Template("AGGREGATE_STRUCT", BoqComponent.RCC_FRAMING, "Aggregate 20mm", BoqCategory.MATERIAL, "cum",
                    EstimationBasis.STRUCTURAL_CONCRETE_M3, new BigDecimal("0.84"), new BigDecimal("3.00"),
                    null, null, null, null),
            new Template("STEEL_STRUCT", BoqComponent.RCC_FRAMING, "TMT Steel Fe500", BoqCategory.MATERIAL, "kg",
                    EstimationBasis.STRUCTURAL_STEEL_KG, BigDecimal.ONE, new BigDecimal("3.00"),
                    null, null, null, null),
            new Template("TILE_DRAWN", BoqComponent.FLOORING, "Vitrified Tile", BoqCategory.MATERIAL, "sqft",
                    EstimationBasis.DRAWN_FLOOR_AREA, BigDecimal.ONE, new BigDecimal("8.00"),
                    null, null, null, null)
    );
}
