package com.buildflow.estimation.service;

import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.estimation.dto.SiteModels.Envelope;
import com.buildflow.estimation.dto.SiteModels.Measure;
import com.buildflow.estimation.dto.SiteModels.SiteCheckResponse;
import com.buildflow.estimation.dto.SiteModels.SiteRulesRequest;
import com.buildflow.estimation.dto.SiteModels.Setbacks;
import com.buildflow.estimation.dto.SiteModels.Violation;
import com.buildflow.estimation.entity.FloorPlan;
import com.buildflow.estimation.entity.FloorRequirement;
import com.buildflow.estimation.entity.HouseRequirement;
import com.buildflow.estimation.entity.PlanRoom;
import com.buildflow.estimation.repository.FloorPlanRepository;
import com.buildflow.estimation.repository.HouseRequirementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks a drawn plan against the site rules the user entered: setbacks, plot coverage and floor area
 * ratio. No legal limits are built in; they differ by authority, town and plot size, so a rule that
 * has not been entered is reported as "not set" rather than assumed. Setbacks alone fall back to
 * planning defaults, because the layout suggester needs some margin to work with.
 */
@Service
public class SiteRulesService {

    static final double DEFAULT_FRONT_FT = 5;
    static final double DEFAULT_REAR_FT = 3;
    static final double DEFAULT_SIDE_FT = 3;
    private static final double TOLERANCE_FT = 0.05;

    private final HouseRequirementRepository houseRequirementRepository;
    private final FloorPlanRepository floorPlanRepository;
    private final CurrentUserProvider currentUserProvider;

    public SiteRulesService(HouseRequirementRepository houseRequirementRepository,
                             FloorPlanRepository floorPlanRepository,
                             CurrentUserProvider currentUserProvider) {
        this.houseRequirementRepository = houseRequirementRepository;
        this.floorPlanRepository = floorPlanRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public SiteCheckResponse check(Long requirementId) {
        return build(findOwned(requirementId));
    }

    @Transactional
    public SiteCheckResponse save(Long requirementId, SiteRulesRequest rules) {
        HouseRequirement requirement = findOwned(requirementId);
        requirement.setSetbackFrontFt(rules.setbackFrontFt());
        requirement.setSetbackRearFt(rules.setbackRearFt());
        requirement.setSetbackLeftFt(rules.setbackLeftFt());
        requirement.setSetbackRightFt(rules.setbackRightFt());
        requirement.setMaxCoveragePercent(rules.maxCoveragePercent());
        requirement.setMaxFar(rules.maxFar());
        return build(houseRequirementRepository.save(requirement));
    }

    /** Setbacks in feet as front, rear, left, right, with defaults for any not entered. */
    static double[] effectiveSetbacks(HouseRequirement requirement) {
        return new double[]{
                orDefault(requirement.getSetbackFrontFt(), DEFAULT_FRONT_FT),
                orDefault(requirement.getSetbackRearFt(), DEFAULT_REAR_FT),
                orDefault(requirement.getSetbackLeftFt(), DEFAULT_SIDE_FT),
                orDefault(requirement.getSetbackRightFt(), DEFAULT_SIDE_FT)};
    }

    private SiteCheckResponse build(HouseRequirement requirement) {
        double plotWidth = requirement.getPlotWidthFt().doubleValue();
        double plotDepth = requirement.getPlotLengthFt().doubleValue();
        double[] sb = effectiveSetbacks(requirement);
        double envX = sb[2];
        double envY = sb[0];
        double envW = plotWidth - sb[2] - sb[3];
        double envD = plotDepth - sb[0] - sb[1];

        List<String> notes = new ArrayList<>();
        if (envW <= 0 || envD <= 0) {
            notes.add("The setbacks leave no buildable area on this plot. Reduce them or check the plot size.");
        }

        Map<Integer, FloorPlan> saved = new HashMap<>();
        for (FloorPlan plan : floorPlanRepository.findByHouseRequirementIdOrderByFloorLevelAsc(requirement.getId())) {
            saved.put(plan.getFloorLevel(), plan);
        }

        List<Violation> violations = new ArrayList<>();
        double groundArea = 0;
        double totalArea = 0;
        boolean fullyDrawn = !requirement.getFloors().isEmpty();
        for (FloorRequirement floor : requirement.getFloors()) {
            FloorPlan plan = saved.get(floor.getFloorLevel());
            double area;
            if (plan != null && !plan.getRooms().isEmpty()) {
                area = 0;
                for (PlanRoom room : plan.getRooms()) {
                    area += room.getWidthFt().doubleValue() * room.getDepthFt().doubleValue();
                    String where = intrusion(room, envX, envY, envW, envD, sb);
                    if (where != null) {
                        violations.add(new Violation(floor.getFloorLevel(),
                                room.getName() != null && !room.getName().isBlank() ? room.getName() : room.getType().name(),
                                "extends into the " + where));
                    }
                }
            } else {
                fullyDrawn = false;
                area = floor.getFloorAreaSqft().doubleValue();
            }
            totalArea += area;
            if (floor.getFloorLevel() == 0) {
                groundArea = area;
            }
        }
        if (!fullyDrawn) {
            notes.add("Some floors have no drawn plan, so their checklist area is used and their rooms cannot be checked against the setbacks.");
        }

        double plotArea = plotWidth * plotDepth;
        Measure coverage = measure(plotArea > 0 ? groundArea / plotArea * 100 : 0, requirement.getMaxCoveragePercent());
        Measure far = measure(plotArea > 0 ? totalArea / plotArea : 0, requirement.getMaxFar());
        if ("NOT_SET".equals(coverage.status()) || "NOT_SET".equals(far.status())) {
            notes.add("Enter the coverage and FAR limits your local authority allows to have them checked.");
        }
        notes.add("This checks the concept drawing against the rules you entered. It is not a statutory approval; confirm "
                + "the actual setbacks, coverage and FAR with the local authority or an architect.");

        boolean compliant = violations.isEmpty() && envW > 0 && envD > 0
                && !"EXCEEDS".equals(coverage.status()) && !"EXCEEDS".equals(far.status());

        SiteRulesRequest rules = new SiteRulesRequest(requirement.getSetbackFrontFt(), requirement.getSetbackRearFt(),
                requirement.getSetbackLeftFt(), requirement.getSetbackRightFt(),
                requirement.getMaxCoveragePercent(), requirement.getMaxFar());
        return new SiteCheckResponse(
                rules,
                new Setbacks(dec(sb[0]), dec(sb[1]), dec(sb[2]), dec(sb[3]),
                        requirement.getSetbackFrontFt() == null, requirement.getSetbackRearFt() == null,
                        requirement.getSetbackLeftFt() == null, requirement.getSetbackRightFt() == null),
                new Envelope(dec(envX), dec(envY), dec(Math.max(envW, 0)), dec(Math.max(envD, 0))),
                dec(plotArea), dec(groundArea), dec(totalArea), coverage, far, fullyDrawn, violations, compliant, notes);
    }

    // Which setback a room breaks, or null when it sits inside the buildable envelope.
    private String intrusion(PlanRoom room, double envX, double envY, double envW, double envD, double[] sb) {
        List<String> sides = new ArrayList<>();
        double x = room.getX().doubleValue();
        double y = room.getY().doubleValue();
        if (y < envY - TOLERANCE_FT) sides.add("front setback");
        if (y + room.getDepthFt().doubleValue() > envY + envD + TOLERANCE_FT) sides.add("rear setback");
        if (x < envX - TOLERANCE_FT) sides.add("left setback");
        if (x + room.getWidthFt().doubleValue() > envX + envW + TOLERANCE_FT) sides.add("right setback");
        return sides.isEmpty() ? null : String.join(" and the ", sides);
    }

    private Measure measure(double value, BigDecimal limit) {
        BigDecimal v = dec(value);
        if (limit == null) {
            return new Measure(v, null, "NOT_SET");
        }
        return new Measure(v, limit, value <= limit.doubleValue() + 0.005 ? "OK" : "EXCEEDS");
    }

    private static double orDefault(BigDecimal value, double fallback) {
        return value != null ? value.doubleValue() : fallback;
    }

    private HouseRequirement findOwned(Long id) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return houseRequirementRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("House requirement not found."));
    }

    private static BigDecimal dec(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
