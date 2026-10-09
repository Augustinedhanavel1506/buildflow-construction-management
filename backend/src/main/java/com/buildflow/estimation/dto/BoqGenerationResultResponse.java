package com.buildflow.estimation.dto;

import com.buildflow.boq.dto.BoqItemResponse;

import java.math.BigDecimal;
import java.util.List;

public record BoqGenerationResultResponse(
        Long houseRequirementId,
        Long projectId,
        BigDecimal totalBuiltupAreaSqft,
        BigDecimal estimatedCost,
        BigDecimal estimatedCostLow,
        BigDecimal estimatedCostHigh,
        List<BoqItemResponse> items,
        List<String> unpricedItems,
        // DRAWN_PLAN when wall, plaster and floor quantities came from the drawn floor plans,
        // otherwise AREA_RULES (built-up-area thumb rules).
        String quantityBasis,
        // DRAWN_STRUCTURE when concrete and steel came from drawn columns, beams, slabs and footings.
        String structuralBasis
) {
}
