package com.buildflow.dealer.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.dealer.dto.DealerQuoteUpdateRequest;
import com.buildflow.dealer.dto.QuoteRequestCreateRequest;
import com.buildflow.dealer.dto.QuoteRequestResponse;
import com.buildflow.dealer.dto.QuoteRequestSummaryResponse;
import com.buildflow.dealer.service.QuoteRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class QuoteRequestController {

    private final QuoteRequestService quoteRequestService;

    public QuoteRequestController(QuoteRequestService quoteRequestService) {
        this.quoteRequestService = quoteRequestService;
    }

    @GetMapping("/api/quote-requests")
    public ResponseEntity<ApiResponse<List<QuoteRequestSummaryResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(quoteRequestService.list()));
    }

    @GetMapping("/api/quote-requests/{id}")
    public ResponseEntity<ApiResponse<QuoteRequestResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(quoteRequestService.get(id)));
    }

    @PostMapping("/api/quote-requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<QuoteRequestResponse>> create(@Valid @RequestBody QuoteRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Quote request created.", quoteRequestService.create(request)));
    }

    @PutMapping("/api/quote-requests/{id}/quotes/{quoteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<QuoteRequestResponse>> recordQuote(
            @PathVariable Long id, @PathVariable Long quoteId, @Valid @RequestBody DealerQuoteUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Quote saved.", quoteRequestService.recordQuote(id, quoteId, request)));
    }

    @PostMapping("/api/quote-requests/{id}/award/{quoteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<QuoteRequestResponse>> award(@PathVariable Long id, @PathVariable Long quoteId) {
        return ResponseEntity.ok(ApiResponse.success("Quote awarded.", quoteRequestService.award(id, quoteId)));
    }

    @PostMapping("/api/quote-requests/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<QuoteRequestResponse>> close(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Quote request closed.", quoteRequestService.close(id)));
    }
}
