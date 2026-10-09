package com.buildflow.dealer.dto;

import com.buildflow.dealer.entity.DealerQuoteStatus;
import com.buildflow.dealer.entity.QuoteRequest;

import java.time.Instant;

public record QuoteRequestSummaryResponse(
        Long id,
        Long projectId,
        String projectName,
        String title,
        String status,
        int dealerCount,
        int receivedCount,
        int itemCount,
        Instant createdAt
) {
    public static QuoteRequestSummaryResponse from(QuoteRequest request) {
        return new QuoteRequestSummaryResponse(
                request.getId(),
                request.getProject().getId(),
                request.getProject().getName(),
                request.getTitle(),
                request.getStatus().name(),
                request.getQuotes().size(),
                (int) request.getQuotes().stream().filter(q -> q.getStatus() == DealerQuoteStatus.RECEIVED).count(),
                request.getItems().size(),
                request.getCreatedAt());
    }
}
