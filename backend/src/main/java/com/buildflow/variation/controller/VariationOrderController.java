package com.buildflow.variation.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.variation.dto.VariationOrderRequest;
import com.buildflow.variation.dto.VariationOrderResponse;
import com.buildflow.variation.service.VariationOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class VariationOrderController {

    private final VariationOrderService variationOrderService;

    public VariationOrderController(VariationOrderService variationOrderService) {
        this.variationOrderService = variationOrderService;
    }

    @GetMapping("/api/projects/{projectId}/variations")
    public ResponseEntity<ApiResponse<List<VariationOrderResponse>>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(variationOrderService.list(projectId)));
    }

    @PostMapping("/api/projects/{projectId}/variations")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<VariationOrderResponse>> create(
            @PathVariable Long projectId, @Valid @RequestBody VariationOrderRequest request) {
        VariationOrderResponse response = variationOrderService.create(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Variation order submitted successfully.", response));
    }

    @PatchMapping("/api/variations/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<VariationOrderResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Variation order approved.", variationOrderService.approve(id)));
    }

    @PatchMapping("/api/variations/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<VariationOrderResponse>> reject(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Variation order rejected.", variationOrderService.reject(id)));
    }
}
