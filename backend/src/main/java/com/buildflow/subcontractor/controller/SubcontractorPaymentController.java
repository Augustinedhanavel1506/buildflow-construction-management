package com.buildflow.subcontractor.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.subcontractor.dto.SubcontractorPaymentRequest;
import com.buildflow.subcontractor.dto.SubcontractorPaymentResponse;
import com.buildflow.subcontractor.service.SubcontractorPaymentService;
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
public class SubcontractorPaymentController {

    private final SubcontractorPaymentService subcontractorPaymentService;

    public SubcontractorPaymentController(SubcontractorPaymentService subcontractorPaymentService) {
        this.subcontractorPaymentService = subcontractorPaymentService;
    }

    @GetMapping("/api/subcontractor-bills/{billId}/payments")
    public ResponseEntity<ApiResponse<List<SubcontractorPaymentResponse>>> list(@PathVariable Long billId) {
        return ResponseEntity.ok(ApiResponse.success(subcontractorPaymentService.list(billId)));
    }

    @PostMapping("/api/subcontractor-bills/{billId}/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<SubcontractorPaymentResponse>> create(
            @PathVariable Long billId, @Valid @RequestBody SubcontractorPaymentRequest request) {
        SubcontractorPaymentResponse response = subcontractorPaymentService.create(billId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment recorded successfully.", response));
    }
}
