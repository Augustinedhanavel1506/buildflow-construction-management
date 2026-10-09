package com.buildflow.variation.dto;

import com.buildflow.variation.entity.VariationOrder;

import java.math.BigDecimal;
import java.time.Instant;

public record VariationOrderResponse(
        Long id,
        Long projectId,
        String title,
        String description,
        String type,
        BigDecimal amount,
        String status,
        String requestedByName,
        String decidedByName,
        Instant createdAt
) {
    public static VariationOrderResponse from(VariationOrder variation) {
        return new VariationOrderResponse(
                variation.getId(),
                variation.getProject().getId(),
                variation.getTitle(),
                variation.getDescription(),
                variation.getType().name(),
                variation.getAmount(),
                variation.getStatus().name(),
                variation.getRequestedBy().getFullName(),
                variation.getDecidedBy() != null ? variation.getDecidedBy().getFullName() : null,
                variation.getCreatedAt()
        );
    }
}
