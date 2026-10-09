package com.buildflow.review.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.review.dto.ReviewRequestCreateRequest;
import com.buildflow.review.dto.ReviewResponse;
import com.buildflow.review.dto.ReviewSubmitRequest;
import com.buildflow.review.service.ReviewRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ReviewRequestController {

    private final ReviewRequestService reviewService;

    public ReviewRequestController(ReviewRequestService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/api/review-requests")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(reviewService.list()));
    }

    @GetMapping("/api/review-requests/{id}")
    public ResponseEntity<ApiResponse<ReviewResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.get(id)));
    }

    @PostMapping("/api/review-requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> create(@Valid @RequestBody ReviewRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review requested.", reviewService.create(request)));
    }

    @PostMapping("/api/review-requests/{id}/submit")
    @PreAuthorize("hasRole('ENGINEER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> submit(
            @PathVariable Long id, @Valid @RequestBody ReviewSubmitRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Review submitted.", reviewService.submit(id, request)));
    }

    @PostMapping("/api/review-requests/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Review cancelled.", reviewService.cancel(id)));
    }
}
