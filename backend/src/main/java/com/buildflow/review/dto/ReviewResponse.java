package com.buildflow.review.dto;

import com.buildflow.boq.dto.BoqItemResponse;
import com.buildflow.estimation.dto.HouseRequirementResponse;
import com.buildflow.estimation.dto.PlanModels;
import com.buildflow.review.entity.ReviewDecision;
import com.buildflow.review.entity.ReviewRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReviewResponse(
        Long id,
        Long projectId,
        String projectName,
        String projectLocation,
        Long engineerId,
        String engineerName,
        String requestedByName,
        String status,
        String message,
        String outcomeNote,
        Instant createdAt,
        Instant completedAt,
        List<Decision> decisions,
        List<BoqItemResponse> items,
        HouseRequirementResponse plan,
        PlanModels.PlansResponse floorPlans
) {
    public record Decision(Long boqItemId, String itemName, BigDecimal originalQuantity,
                           BigDecimal correctedQuantity, String comment) {
        static Decision from(ReviewDecision d) {
            return new Decision(d.getBoqItemId(), d.getItemName(), d.getOriginalQuantity(),
                    d.getCorrectedQuantity(), d.getComment());
        }
    }

    // List view: no BOQ lines or plan, so it stays small.
    public static ReviewResponse summary(ReviewRequest r) {
        return build(r, List.of(), null, null);
    }

    public static ReviewResponse detail(ReviewRequest r, List<BoqItemResponse> items, HouseRequirementResponse plan,
                                        PlanModels.PlansResponse floorPlans) {
        return build(r, items, plan, floorPlans);
    }

    private static ReviewResponse build(ReviewRequest r, List<BoqItemResponse> items, HouseRequirementResponse plan,
                                        PlanModels.PlansResponse floorPlans) {
        return new ReviewResponse(
                r.getId(),
                r.getProject().getId(),
                r.getProject().getName(),
                r.getProject().getLocation(),
                r.getEngineer().getId(),
                r.getEngineer().getFullName(),
                r.getRequestedBy().getFullName(),
                r.getStatus().name(),
                r.getMessage(),
                r.getOutcomeNote(),
                r.getCreatedAt(),
                r.getCompletedAt(),
                r.getDecisions().stream().map(Decision::from).toList(),
                items,
                plan,
                floorPlans);
    }
}
