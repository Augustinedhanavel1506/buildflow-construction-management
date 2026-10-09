package com.buildflow.dealer.service;

import com.buildflow.boq.entity.BoqCategory;
import com.buildflow.boq.entity.BoqItem;
import com.buildflow.boq.repository.BoqItemRepository;
import com.buildflow.business.entity.Business;
import com.buildflow.common.exception.BadRequestException;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.dealer.dto.DealerQuoteUpdateRequest;
import com.buildflow.dealer.dto.QuoteRequestCreateRequest;
import com.buildflow.dealer.dto.QuoteRequestResponse;
import com.buildflow.dealer.dto.QuoteRequestSummaryResponse;
import com.buildflow.dealer.entity.Dealer;
import com.buildflow.dealer.entity.DealerQuote;
import com.buildflow.dealer.entity.DealerQuoteLine;
import com.buildflow.dealer.entity.DealerQuoteStatus;
import com.buildflow.dealer.entity.DealerRate;
import com.buildflow.dealer.entity.QuoteLineSource;
import com.buildflow.dealer.entity.QuoteRequest;
import com.buildflow.dealer.entity.QuoteRequestItem;
import com.buildflow.dealer.entity.QuoteRequestStatus;
import com.buildflow.dealer.repository.DealerRepository;
import com.buildflow.dealer.repository.QuoteRequestRepository;
import com.buildflow.project.entity.Project;
import com.buildflow.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class QuoteRequestService {

    private final QuoteRequestRepository quoteRequestRepository;
    private final DealerRepository dealerRepository;
    private final ProjectRepository projectRepository;
    private final BoqItemRepository boqItemRepository;
    private final CurrentUserProvider currentUserProvider;

    public QuoteRequestService(QuoteRequestRepository quoteRequestRepository,
                                DealerRepository dealerRepository,
                                ProjectRepository projectRepository,
                                BoqItemRepository boqItemRepository,
                                CurrentUserProvider currentUserProvider) {
        this.quoteRequestRepository = quoteRequestRepository;
        this.dealerRepository = dealerRepository;
        this.projectRepository = projectRepository;
        this.boqItemRepository = boqItemRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public QuoteRequestResponse create(QuoteRequestCreateRequest request) {
        Business business = currentUserProvider.getCurrentUser().getBusiness();
        Project project = projectRepository.findByIdAndBusinessId(request.projectId(), business.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));

        // The same material can appear on several BOQ lines (e.g. cement for foundation and RCC),
        // but a dealer quotes one rate per item, so the request asks for the combined quantity.
        Map<String, QuoteRequestItem> itemsByKey = new LinkedHashMap<>();
        for (BoqItem boqItem : boqItemRepository.findByProjectIdOrderByCreatedAtAsc(project.getId())) {
            if (boqItem.getCategory() != BoqCategory.MATERIAL) {
                continue;
            }
            String key = key(boqItem.getItemName(), boqItem.getUnit());
            QuoteRequestItem item = itemsByKey.get(key);
            if (item == null) {
                item = new QuoteRequestItem();
                item.setItemName(boqItem.getItemName());
                item.setUnit(boqItem.getUnit());
                item.setQuantity(BigDecimal.ZERO);
                itemsByKey.put(key, item);
            }
            item.setQuantity(item.getQuantity().add(boqItem.getQuantity()));
        }
        if (itemsByKey.isEmpty()) {
            throw new BadRequestException("This project has no material BOQ items to request quotes for. Generate a BOQ first.");
        }

        QuoteRequest quoteRequest = new QuoteRequest();
        quoteRequest.setBusiness(business);
        quoteRequest.setProject(project);
        quoteRequest.setTitle(request.title().trim());
        for (QuoteRequestItem item : itemsByKey.values()) {
            item.setQuoteRequest(quoteRequest);
            quoteRequest.getItems().add(item);
        }

        for (Long dealerId : new LinkedHashSet<>(request.dealerIds())) {
            Dealer dealer = dealerRepository.findByIdAndBusinessId(dealerId, business.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Dealer not found."));
            if (!dealer.isActive()) {
                throw new BadRequestException("Dealer " + dealer.getName() + " is inactive.");
            }
            quoteRequest.getQuotes().add(newQuote(quoteRequest, dealer));
        }

        return QuoteRequestResponse.from(quoteRequestRepository.save(quoteRequest));
    }

    @Transactional(readOnly = true)
    public List<QuoteRequestSummaryResponse> list() {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return quoteRequestRepository.findByBusinessIdOrderByCreatedAtDesc(businessId).stream()
                .map(QuoteRequestSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuoteRequestResponse get(Long id) {
        return QuoteRequestResponse.from(findOwned(id));
    }

    @Transactional
    public QuoteRequestResponse recordQuote(Long requestId, Long quoteId, DealerQuoteUpdateRequest update) {
        QuoteRequest request = findOwned(requestId);
        requireOpen(request);

        DealerQuote quote = request.getQuotes().stream()
                .filter(q -> q.getId().equals(quoteId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Quote not found."));

        Map<String, DealerQuoteLine> linesByItem = new HashMap<>();
        for (DealerQuoteLine line : quote.getLines()) {
            linesByItem.put(line.getItemName().trim().toLowerCase(Locale.ROOT), line);
        }
        if (update.lines() != null) {
            for (DealerQuoteUpdateRequest.Line incoming : update.lines()) {
                DealerQuoteLine line = linesByItem.get(incoming.itemName().trim().toLowerCase(Locale.ROOT));
                if (line == null) {
                    throw new BadRequestException("Item is not part of this request: " + incoming.itemName());
                }
                line.setUnitRate(incoming.unitRate());
                line.setSource(incoming.unitRate() == null ? QuoteLineSource.INDICATIVE : QuoteLineSource.QUOTED);
            }
        }

        quote.setStatus(update.status());
        quote.setDeliveryCharge(update.deliveryCharge() != null ? update.deliveryCharge() : BigDecimal.ZERO);
        quote.setLoadingCharge(update.loadingCharge() != null ? update.loadingCharge() : BigDecimal.ZERO);
        quote.setNotes(update.notes());
        quote.setReceivedAt(update.status() == DealerQuoteStatus.RECEIVED ? Instant.now() : null);

        return QuoteRequestResponse.from(quoteRequestRepository.save(request));
    }

    @Transactional
    public QuoteRequestResponse award(Long requestId, Long quoteId) {
        QuoteRequest request = findOwned(requestId);
        requireOpen(request);

        DealerQuote quote = request.getQuotes().stream()
                .filter(q -> q.getId().equals(quoteId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Quote not found."));
        if (quote.getStatus() != DealerQuoteStatus.RECEIVED) {
            throw new BadRequestException("Only a received quote can be awarded.");
        }

        request.setAwardedQuoteId(quote.getId());
        request.setStatus(QuoteRequestStatus.AWARDED);
        return QuoteRequestResponse.from(quoteRequestRepository.save(request));
    }

    @Transactional
    public QuoteRequestResponse close(Long requestId) {
        QuoteRequest request = findOwned(requestId);
        requireOpen(request);
        request.setStatus(QuoteRequestStatus.CLOSED);
        return QuoteRequestResponse.from(quoteRequestRepository.save(request));
    }

    // Prefill from the dealer's rate card where the unit matches, marked INDICATIVE so it is never
    // mistaken for a real quotation.
    private DealerQuote newQuote(QuoteRequest request, Dealer dealer) {
        DealerQuote quote = new DealerQuote();
        quote.setQuoteRequest(request);
        quote.setDealer(dealer);

        Map<String, DealerRate> rateCard = new HashMap<>();
        for (DealerRate rate : dealer.getRates()) {
            rateCard.put(rate.getItemName().trim().toLowerCase(Locale.ROOT), rate);
        }
        for (QuoteRequestItem item : request.getItems()) {
            DealerQuoteLine line = new DealerQuoteLine();
            line.setDealerQuote(quote);
            line.setItemName(item.getItemName());
            DealerRate rate = rateCard.get(item.getItemName().trim().toLowerCase(Locale.ROOT));
            if (rate != null && rate.getUnit().equalsIgnoreCase(item.getUnit())) {
                line.setUnitRate(rate.getRate());
            }
            line.setSource(QuoteLineSource.INDICATIVE);
            quote.getLines().add(line);
        }
        return quote;
    }

    private void requireOpen(QuoteRequest request) {
        if (request.getStatus() != QuoteRequestStatus.OPEN) {
            throw new BadRequestException("This quote request is already " + request.getStatus().name().toLowerCase() + ".");
        }
    }

    private QuoteRequest findOwned(Long id) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return quoteRequestRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Quote request not found."));
    }

    private static String key(String name, String unit) {
        return name.trim().toLowerCase(Locale.ROOT) + "|" + unit.trim().toLowerCase(Locale.ROOT);
    }
}
