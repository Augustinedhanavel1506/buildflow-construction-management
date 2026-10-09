package com.buildflow.estimation.service;

import com.buildflow.boq.entity.BoqItem;
import com.buildflow.boq.entity.EstimateSource;
import com.buildflow.boq.repository.BoqItemRepository;
import com.buildflow.common.exception.BadRequestException;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.estimation.dto.BoqGenerationResultResponse;
import com.buildflow.estimation.entity.BoqComponent;
import com.buildflow.estimation.entity.BoqGenerationRule;
import com.buildflow.estimation.entity.EstimationBasis;
import com.buildflow.estimation.entity.FloorRequirement;
import com.buildflow.estimation.entity.HouseRequirement;
import com.buildflow.estimation.entity.HouseRequirementStatus;
import com.buildflow.estimation.repository.BoqGenerationRuleRepository;
import com.buildflow.estimation.repository.HouseRequirementRepository;
import com.buildflow.project.entity.Project;
import com.buildflow.project.repository.ProjectRepository;
import com.buildflow.ratemaster.entity.RateMasterItem;
import com.buildflow.ratemaster.repository.RateMasterItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Turns a HouseRequirement (plot + room checklist) into preliminary BoqItem quantities using
 * configurable BoqGenerationRule coefficients. Deterministic on purpose: no LLM in this path, so
 * every quantity traces back to a specific rule row an admin can inspect and edit.
 */
@Service
public class BoqGenerationService {

    private static final Set<String> INTEGER_UNITS = Set.of("bag", "bags", "nos", "no", "unit", "units", "day", "days");

    private final HouseRequirementRepository houseRequirementRepository;
    private final BoqGenerationRuleRepository ruleRepository;
    private final BoqItemRepository boqItemRepository;
    private final RateMasterItemRepository rateMasterItemRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserProvider currentUserProvider;
    private final FloorPlanService floorPlanService;

    public BoqGenerationService(HouseRequirementRepository houseRequirementRepository,
                                 BoqGenerationRuleRepository ruleRepository,
                                 BoqItemRepository boqItemRepository,
                                 RateMasterItemRepository rateMasterItemRepository,
                                 ProjectRepository projectRepository,
                                 CurrentUserProvider currentUserProvider,
                                 FloorPlanService floorPlanService) {
        this.floorPlanService = floorPlanService;
        this.houseRequirementRepository = houseRequirementRepository;
        this.ruleRepository = ruleRepository;
        this.boqItemRepository = boqItemRepository;
        this.rateMasterItemRepository = rateMasterItemRepository;
        this.projectRepository = projectRepository;
        this.currentUserProvider = currentUserProvider;
    }

    private record Line(BoqGenerationRule rule, BigDecimal quantity, BigDecimal low, BigDecimal high,
                        BigDecimal rate, BigDecimal amount) {
    }

    private record Estimate(List<Line> lines, List<BoqGenerationRule> unpricedRules, BasisValues basis) {
    }

    /** One priced line of an estimate calculated without saving anything (used for comparisons). */
    public record ComparisonLine(String itemName, String unit, String component, BigDecimal quantity, BigDecimal amount) {
    }

    public record ComparisonEstimate(BigDecimal total, BigDecimal builtUpAreaSqft, List<ComparisonLine> lines,
                                     List<String> unpricedItems) {
    }

    @Transactional
    public BoqGenerationResultResponse generate(Long houseRequirementId) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        HouseRequirement requirement = houseRequirementRepository.findByIdAndBusinessId(houseRequirementId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("House requirement not found."));

        PlanGeometry.Derived drawn = floorPlanService.deriveIfFullyDrawn(requirement);
        StructureGeometry.Quantities structure = floorPlanService.structureIfComplete(requirement);

        // A rule an engineer has already validated on this project is not generated again, or the
        // validated line would sit next to a fresh preliminary duplicate of itself.
        Set<String> validatedRuleCodes = boqItemRepository
                .findByProjectIdOrderByCreatedAtAsc(requirement.getProject().getId()).stream()
                .filter(i -> i.getEstimateSource() == EstimateSource.ENGINEER_VALIDATED && i.getSourceRuleCode() != null)
                .map(BoqItem::getSourceRuleCode)
                .collect(java.util.stream.Collectors.toSet());

        Estimate estimate = estimate(requirement, businessId, drawn, structure, validatedRuleCodes);

        // Regenerating replaces only what the system produced last time; engineer-validated or
        // manually-added lines on the same project are left untouched.
        boqItemRepository.deleteByProjectIdAndEstimateSource(requirement.getProject().getId(), EstimateSource.SYSTEM_PRELIMINARY);

        List<BoqItem> generated = new ArrayList<>();
        for (Line line : estimate.lines()) {
            BoqGenerationRule rule = line.rule();
            BoqItem item = new BoqItem();
            item.setProject(requirement.getProject());
            item.setItemName(rule.getItemName());
            item.setCategory(rule.getBoqCategory());
            item.setUnit(rule.getUnit());
            item.setQuantity(line.quantity());
            item.setQuantityLow(line.low());
            item.setQuantityHigh(line.high());
            item.setRate(line.rate());
            item.setEstimatedAmount(line.amount());
            item.setActualAmount(BigDecimal.ZERO);
            item.setEstimateSource(EstimateSource.SYSTEM_PRELIMINARY);
            item.setSourceRuleId(rule.getId());
            item.setSourceRuleVersion(rule.getVersion());
            item.setSourceRuleCode(rule.getRuleCode());
            item.setComponent(rule.getComponent().name());
            generated.add(item);
        }
        boqItemRepository.saveAll(generated);
        List<BoqItem> projectItems = boqItemRepository.findByProjectIdOrderByCreatedAtAsc(requirement.getProject().getId());

        BigDecimal totalEstimated = projectItems.stream()
                .map(BoqItem::getEstimatedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLow = sumBand(projectItems, BoqItem::getQuantityLow);
        BigDecimal totalHigh = sumBand(projectItems, BoqItem::getQuantityHigh);

        Project project = requirement.getProject();
        project.setEstimatedCost(totalEstimated);
        projectRepository.save(project);

        requirement.setStatus(HouseRequirementStatus.ESTIMATED);
        houseRequirementRepository.save(requirement);

        return new BoqGenerationResultResponse(
                requirement.getId(),
                project.getId(),
                estimate.basis().totalBuiltupArea,
                totalEstimated,
                totalLow,
                totalHigh,
                projectItems.stream().map(com.buildflow.boq.dto.BoqItemResponse::from).toList(),
                estimate.unpricedRules().stream().map(BoqGenerationRule::getItemName).distinct().sorted().toList(),
                drawn != null ? "DRAWN_PLAN" : "AREA_RULES",
                structure != null ? "DRAWN_STRUCTURE" : "AREA_RULES");
    }

    /**
     * Calculates an estimate for a requirement that may exist only in memory (a what-if copy), using
     * the built-up-area rules so two scenarios are always compared by the same method. Saves nothing.
     */
    @Transactional(readOnly = true)
    public ComparisonEstimate comparisonEstimate(HouseRequirement requirement) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        Estimate estimate = estimate(requirement, businessId, null, null, Set.of());
        List<ComparisonLine> lines = estimate.lines().stream()
                .map(l -> new ComparisonLine(l.rule().getItemName(), l.rule().getUnit(),
                        l.rule().getComponent().name(), l.quantity(), l.amount()))
                .toList();
        BigDecimal total = estimate.lines().stream().map(Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ComparisonEstimate(total, estimate.basis().totalBuiltupArea, lines,
                estimate.unpricedRules().stream().map(BoqGenerationRule::getItemName).distinct().sorted().toList());
    }

    private Estimate estimate(HouseRequirement requirement, Long businessId, PlanGeometry.Derived drawn,
                              StructureGeometry.Quantities structure, Set<String> excludedRuleCodes) {
        if (requirement.getFloors().isEmpty()) {
            throw new BadRequestException("Add at least one floor before generating an estimate.");
        }

        BasisValues basisValues = computeBasisValues(requirement, drawn, structure);

        List<BoqGenerationRule> matched = findMatchingRules(requirement, basisValues.floorCount, businessId, drawn != null,
                structure != null);
        if (matched.isEmpty()) {
            throw new BadRequestException(
                    "No BOQ generation rules are configured for this grade/specification combination yet.");
        }

        // The requirement's district rate wins; items it has no rate for fall back to the default rate.
        String district = requirement.getDistrict();
        Map<String, RateMasterItem> rateByItemName = new java.util.HashMap<>();
        for (RateMasterItem item : rateMasterItemRepository.findByBusinessIdAndActiveTrueOrderByItemNameAsc(businessId)) {
            boolean isDefault = item.getDistrict() == null;
            boolean isLocal = district != null && !district.isBlank() && district.trim().equalsIgnoreCase(item.getDistrict());
            if (isLocal || (isDefault && !rateByItemName.containsKey(rateKey(item.getItemName())))) {
                rateByItemName.put(rateKey(item.getItemName()), item);
            }
        }

        List<String> missingRates = matched.stream()
                .map(BoqGenerationRule::getItemName)
                .distinct()
                .filter(name -> !rateByItemName.containsKey(rateKey(name)))
                .sorted()
                .toList();
        // Lines without a rate are left out (never priced at zero) and reported back so the caller
        // can show what's missing. Only fail when nothing at all can be priced.
        List<BoqGenerationRule> unpricedRules = matched.stream()
                .filter(rule -> !rateByItemName.containsKey(rateKey(rule.getItemName())))
                .toList();
        if (unpricedRules.size() == matched.size()) {
            throw new BadRequestException(
                    "Add rate master entries before generating an estimate. Missing rates for: "
                            + String.join(", ", missingRates));
        }

        List<Line> lines = new ArrayList<>();
        for (BoqGenerationRule rule : matched) {
            if (!rateByItemName.containsKey(rateKey(rule.getItemName())) || excludedRuleCodes.contains(rule.getRuleCode())) {
                continue;
            }
            BigDecimal basisValue = resolveBasis(rule.getBasis(), basisValues);
            BigDecimal wastageMultiplier = BigDecimal.ONE.add(
                    rule.getWastagePercent().divide(BigDecimal.valueOf(100)));

            BigDecimal quantity = roundForUnit(
                    rule.getCoefficient().multiply(basisValue).multiply(wastageMultiplier), rule.getUnit());
            BigDecimal quantityLow = rule.getMinCoefficient() == null ? null : roundForUnit(
                    rule.getMinCoefficient().multiply(basisValue).multiply(wastageMultiplier), rule.getUnit());
            BigDecimal quantityHigh = rule.getMaxCoefficient() == null ? null : roundForUnit(
                    rule.getMaxCoefficient().multiply(basisValue).multiply(wastageMultiplier), rule.getUnit());

            // A rule whose basis is zero for this plan (no doors, no bathrooms) would only add an empty line.
            if (quantity.signum() == 0) {
                continue;
            }
            BigDecimal rate = rateByItemName.get(rateKey(rule.getItemName())).getStandardRate();
            BigDecimal amount = quantity.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            lines.add(new Line(rule, quantity, quantityLow, quantityHigh, rate, amount));
        }
        return new Estimate(lines, unpricedRules, basisValues);
    }

    // Lines without a min/max band (fixed counts, finishes) contribute their point quantity to
    // both bounds, so the total range reflects only the genuinely uncertain lines.
    private BigDecimal sumBand(List<BoqItem> items, java.util.function.Function<BoqItem, BigDecimal> bandSelector) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BoqItem item : items) {
            BigDecimal qty = bandSelector.apply(item) != null ? bandSelector.apply(item) : item.getQuantity();
            sum = sum.add(qty.multiply(item.getRate()));
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    private List<BoqGenerationRule> findMatchingRules(HouseRequirement requirement, int floorCount, Long businessId,
                                                      boolean drawn, boolean structural) {
        List<BoqGenerationRule> candidates = ruleRepository.findApplicable(
                requirement.getConstructionGrade(), LocalDate.now(), businessId);

        // Prefer a business-specific override over the platform default with the same rule_code.
        Map<String, BoqGenerationRule> effective = new LinkedHashMap<>();
        for (BoqGenerationRule rule : candidates) {
            effective.merge(baseCode(rule), rule,
                    (existing, incoming) -> incoming.getBusiness() != null ? incoming : existing);
        }

        return effective.values().stream()
                .filter(rule -> rule.getStructureType() == null || rule.getStructureType() == requirement.getStructureType())
                .filter(rule -> rule.getWallMaterial() == null || rule.getWallMaterial() == requirement.getWallMaterial())
                .filter(rule -> rule.getRoofType() == null || rule.getRoofType() == requirement.getRoofType())
                .filter(rule -> rule.getApplicableFloorMin() == null || floorCount >= rule.getApplicableFloorMin())
                .filter(rule -> rule.getApplicableFloorMax() == null || floorCount <= rule.getApplicableFloorMax())
                // With drawings, wall/plaster/paint/floor materials come from the drawn rules, not
                // the built-up-area thumb rules; without drawings it is the other way round.
                .filter(rule -> drawn ? !isAreaThumbRule(rule) : !isDrawnBasis(rule.getBasis()))
                // Likewise the RCC frame: drawn columns, beams and slabs replace the per-sq.ft concrete rules.
                .filter(rule -> structural ? !isFrameThumbRule(rule) : !isStructuralBasis(rule.getBasis()))
                .sorted(Comparator.comparing(BoqGenerationRule::getComponent))
                .toList();
    }

    // Rate Master names are typed by users, so match them ignoring case and surrounding spaces.
    private static String rateKey(String itemName) {
        return itemName.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static boolean isDrawnBasis(EstimationBasis basis) {
        return basis == EstimationBasis.EXTERNAL_WALL_AREA || basis == EstimationBasis.INTERNAL_WALL_AREA
                || basis == EstimationBasis.PLASTER_AREA || basis == EstimationBasis.DRAWN_FLOOR_AREA;
    }

    private static boolean isStructuralBasis(EstimationBasis basis) {
        return basis == EstimationBasis.STRUCTURAL_CONCRETE_M3 || basis == EstimationBasis.STRUCTURAL_STEEL_KG;
    }

    // Material rules that approximate the RCC frame and foundation from floor area.
    private static boolean isFrameThumbRule(BoqGenerationRule rule) {
        return rule.getBoqCategory() == com.buildflow.boq.entity.BoqCategory.MATERIAL
                && (rule.getComponent() == BoqComponent.RCC_FRAMING || rule.getComponent() == BoqComponent.FOUNDATION)
                && (rule.getBasis() == EstimationBasis.TOTAL_BUILTUP_AREA || rule.getBasis() == EstimationBasis.GROUND_FLOOR_AREA);
    }

    // Material rules that approximate walls, plaster, paint and flooring from built-up area.
    private static boolean isAreaThumbRule(BoqGenerationRule rule) {
        return rule.getBoqCategory() == com.buildflow.boq.entity.BoqCategory.MATERIAL
                && rule.getBasis() == EstimationBasis.TOTAL_BUILTUP_AREA
                && (rule.getComponent() == BoqComponent.MASONRY || rule.getComponent() == BoqComponent.PLASTERING
                || rule.getComponent() == BoqComponent.PAINTING || rule.getComponent() == BoqComponent.FLOORING);
    }

    private static String baseCode(BoqGenerationRule rule) {
        return rule.getBaseRuleCode() != null ? rule.getBaseRuleCode() : rule.getRuleCode();
    }

    private BasisValues computeBasisValues(HouseRequirement requirement, PlanGeometry.Derived drawn,
                                           StructureGeometry.Quantities structure) {
        List<FloorRequirement> floors = requirement.getFloors();

        BigDecimal totalBuiltupArea = floors.stream()
                .map(FloorRequirement::getFloorAreaSqft)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal groundFloorArea = floors.stream()
                .min(Comparator.comparingInt(FloorRequirement::getFloorLevel))
                .map(FloorRequirement::getFloorAreaSqft)
                .orElse(BigDecimal.ZERO);

        int floorCount = new TreeSet<>(floors.stream().map(FloorRequirement::getFloorLevel).toList()).size();

        int roomCount = floors.stream().mapToInt(f ->
                f.getBedroomCount()
                        + (f.isHasKitchen() ? 1 : 0)
                        + (f.isHasHall() ? 1 : 0)
                        + (f.isHasPoojaRoom() ? 1 : 0)
        ).sum();
        int bathroomCount = floors.stream().mapToInt(FloorRequirement::getBathroomCount).sum();
        int doorCount = floors.stream().mapToInt(FloorRequirement::getDoorCount).sum();
        int windowCount = floors.stream().mapToInt(FloorRequirement::getWindowCount).sum();

        BigDecimal external = drawn == null ? BigDecimal.ZERO : BigDecimal.valueOf(drawn.externalWallAreaSqft());
        BigDecimal internal = drawn == null ? BigDecimal.ZERO : BigDecimal.valueOf(drawn.internalWallAreaSqft());
        BigDecimal plaster = drawn == null ? BigDecimal.ZERO : BigDecimal.valueOf(drawn.plasterAreaSqft());
        BigDecimal drawnFloor = drawn == null ? BigDecimal.ZERO : BigDecimal.valueOf(drawn.roomsAreaSqft());
        return new BasisValues(totalBuiltupArea, groundFloorArea, floorCount, roomCount, bathroomCount, doorCount, windowCount,
                external, internal, plaster, drawnFloor,
                structure == null ? BigDecimal.ZERO : BigDecimal.valueOf(structure.concreteM3()),
                structure == null ? BigDecimal.ZERO : BigDecimal.valueOf(structure.steelKg()));
    }

    private BigDecimal resolveBasis(EstimationBasis basis, BasisValues values) {
        return switch (basis) {
            case TOTAL_BUILTUP_AREA -> values.totalBuiltupArea;
            case GROUND_FLOOR_AREA -> values.groundFloorArea;
            case FLOOR_COUNT -> BigDecimal.valueOf(values.floorCount);
            case ROOM_COUNT -> BigDecimal.valueOf(values.roomCount);
            case BATHROOM_COUNT -> BigDecimal.valueOf(values.bathroomCount);
            case DOOR_COUNT -> BigDecimal.valueOf(values.doorCount);
            case WINDOW_COUNT -> BigDecimal.valueOf(values.windowCount);
            case FIXED -> BigDecimal.ONE;
            case EXTERNAL_WALL_AREA -> values.externalWallArea;
            case INTERNAL_WALL_AREA -> values.internalWallArea;
            case PLASTER_AREA -> values.plasterArea;
            case DRAWN_FLOOR_AREA -> values.drawnFloorArea;
            case STRUCTURAL_CONCRETE_M3 -> values.structuralConcreteM3;
            case STRUCTURAL_STEEL_KG -> values.structuralSteelKg;
        };
    }

    private BigDecimal roundForUnit(BigDecimal value, String unit) {
        if (unit != null && INTEGER_UNITS.contains(unit.trim().toLowerCase())) {
            return value.setScale(0, RoundingMode.CEILING);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private record BasisValues(
            BigDecimal totalBuiltupArea,
            BigDecimal groundFloorArea,
            int floorCount,
            int roomCount,
            int bathroomCount,
            int doorCount,
            int windowCount,
            BigDecimal externalWallArea,
            BigDecimal internalWallArea,
            BigDecimal plasterArea,
            BigDecimal drawnFloorArea,
            BigDecimal structuralConcreteM3,
            BigDecimal structuralSteelKg
    ) {
    }
}
