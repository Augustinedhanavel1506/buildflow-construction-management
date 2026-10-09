package com.buildflow.dealer.dto;

import com.buildflow.dealer.entity.DealerQuote;
import com.buildflow.dealer.entity.DealerQuoteLine;
import com.buildflow.dealer.entity.QuoteLineSource;
import com.buildflow.dealer.entity.DealerQuoteStatus;
import com.buildflow.dealer.entity.QuoteRequest;
import com.buildflow.dealer.entity.QuoteRequestItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record QuoteRequestResponse(
        Long id,
        Long projectId,
        String projectName,
        String title,
        String status,
        Long awardedQuoteId,
        Long lowestCompleteQuoteId,
        Instant createdAt,
        List<Item> items,
        List<Quote> quotes
) {
    public record Item(String itemName, String unit, BigDecimal quantity) {
    }

    public record Line(String itemName, String unit, BigDecimal quantity, BigDecimal unitRate,
                       BigDecimal amount, String source) {
    }

    // materialTotal covers only priced lines; effectiveTotal adds delivery and loading, which is
    // what actually compares dealers (a cheaper rate can lose to free delivery).
    public record Quote(Long id, Long dealerId, String dealerName, String dealerPhone, String status,
                        BigDecimal deliveryCharge, BigDecimal loadingCharge, String notes, Instant receivedAt,
                        List<Line> lines, int pricedItemCount, int itemCount, boolean complete,
                        BigDecimal materialTotal, BigDecimal effectiveTotal) {
    }

    public static QuoteRequestResponse from(QuoteRequest request) {
        List<Quote> quotes = request.getQuotes().stream().map(q -> toQuote(q, request.getItems())).toList();

        // Only real (RECEIVED) quotes covering every item are eligible, so prefilled rate-card
        // estimates never get flagged as "the lowest quote".
        Long lowest = quotes.stream()
                .filter(q -> q.complete() && DealerQuoteStatus.RECEIVED.name().equals(q.status()))
                .min(Comparator.comparing(Quote::effectiveTotal))
                .map(Quote::id)
                .orElse(null);

        return new QuoteRequestResponse(
                request.getId(),
                request.getProject().getId(),
                request.getProject().getName(),
                request.getTitle(),
                request.getStatus().name(),
                request.getAwardedQuoteId(),
                lowest,
                request.getCreatedAt(),
                request.getItems().stream()
                        .map(i -> new Item(i.getItemName(), i.getUnit(), i.getQuantity())).toList(),
                quotes);
    }

    private static Quote toQuote(DealerQuote quote, List<QuoteRequestItem> items) {
        Map<String, DealerQuoteLine> linesByItem = new HashMap<>();
        for (DealerQuoteLine line : quote.getLines()) {
            linesByItem.put(line.getItemName().trim().toLowerCase(), line);
        }

        BigDecimal materialTotal = BigDecimal.ZERO;
        int priced = 0;
        List<Line> lines = new java.util.ArrayList<>();
        for (QuoteRequestItem item : items) {
            DealerQuoteLine line = linesByItem.get(item.getItemName().trim().toLowerCase());
            BigDecimal rate = line == null ? null : line.getUnitRate();
            BigDecimal amount = rate == null ? null : item.getQuantity().multiply(rate).setScale(2, RoundingMode.HALF_UP);
            if (amount != null) {
                materialTotal = materialTotal.add(amount);
                priced++;
            }
            QuoteLineSource source = line == null ? QuoteLineSource.INDICATIVE : line.getSource();
            lines.add(new Line(item.getItemName(), item.getUnit(), item.getQuantity(), rate, amount, source.name()));
        }

        BigDecimal effective = materialTotal.add(quote.getDeliveryCharge()).add(quote.getLoadingCharge());
        return new Quote(
                quote.getId(),
                quote.getDealer().getId(),
                quote.getDealer().getName(),
                quote.getDealer().getPhone(),
                quote.getStatus().name(),
                quote.getDeliveryCharge(),
                quote.getLoadingCharge(),
                quote.getNotes(),
                quote.getReceivedAt(),
                lines,
                priced,
                items.size(),
                priced == items.size(),
                materialTotal.setScale(2, RoundingMode.HALF_UP),
                effective.setScale(2, RoundingMode.HALF_UP));
    }
}
