package com.buildflow.estimation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.List;

public final class SiteModels {

    private SiteModels() {
    }

    // Any field left null clears that rule: setbacks then fall back to the planning defaults and an
    // unset coverage or FAR limit is simply not checked.
    public record SiteRulesRequest(
            @DecimalMin(value = "0", message = "Front setback cannot be negative") @DecimalMax(value = "40", message = "Front setback is too large") BigDecimal setbackFrontFt,
            @DecimalMin(value = "0", message = "Rear setback cannot be negative") @DecimalMax(value = "40", message = "Rear setback is too large") BigDecimal setbackRearFt,
            @DecimalMin(value = "0", message = "Left setback cannot be negative") @DecimalMax(value = "40", message = "Left setback is too large") BigDecimal setbackLeftFt,
            @DecimalMin(value = "0", message = "Right setback cannot be negative") @DecimalMax(value = "40", message = "Right setback is too large") BigDecimal setbackRightFt,
            @DecimalMin(value = "1", message = "Maximum coverage must be at least 1%") @DecimalMax(value = "100", message = "Maximum coverage cannot exceed 100%") BigDecimal maxCoveragePercent,
            @DecimalMin(value = "0.1", message = "Maximum FAR must be at least 0.1") @DecimalMax(value = "10", message = "Maximum FAR is too large") BigDecimal maxFar
    ) {
    }

    public record Envelope(BigDecimal x, BigDecimal y, BigDecimal widthFt, BigDecimal depthFt) {
    }

    public record Setbacks(BigDecimal frontFt, BigDecimal rearFt, BigDecimal leftFt, BigDecimal rightFt,
                           boolean frontAssumed, boolean rearAssumed, boolean leftAssumed, boolean rightAssumed) {
    }

    public record Violation(int floorLevel, String roomName, String message) {
    }

    // limit is null and status NOT_SET when the user has not entered that rule.
    public record Measure(BigDecimal value, BigDecimal limit, String status) {
    }

    public record SiteCheckResponse(
            SiteRulesRequest rules,
            Setbacks setbacks,
            Envelope buildableEnvelope,
            BigDecimal plotAreaSqft,
            BigDecimal groundAreaSqft,
            BigDecimal totalAreaSqft,
            Measure coverage,
            Measure far,
            boolean fullyDrawn,
            List<Violation> violations,
            boolean compliant,
            List<String> notes
    ) {
    }
}
