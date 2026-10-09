package com.buildflow.subcontractor.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.subcontractor.dto.SubcontractorBillRequest;
import com.buildflow.subcontractor.dto.SubcontractorBillResponse;
import com.buildflow.subcontractor.service.SubcontractorBillService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SubcontractorBillController {

    private final SubcontractorBillService subcontractorBillService;

    public SubcontractorBillController(SubcontractorBillService subcontractorBillService) {
        this.subcontractorBillService = subcontractorBillService;
    }

    @GetMapping("/api/subcontract-work-orders/{workOrderId}/bills")
    public ResponseEntity<ApiResponse<List<SubcontractorBillResponse>>> list(@PathVariable Long workOrderId) {
        return ResponseEntity.ok(ApiResponse.success(subcontractorBillService.list(workOrderId)));
    }

    @PostMapping("/api/subcontract-work-orders/{workOrderId}/bills")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<SubcontractorBillResponse>> create(
            @PathVariable Long workOrderId, @Valid @RequestBody SubcontractorBillRequest request) {
        SubcontractorBillResponse response = subcontractorBillService.create(workOrderId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subcontractor bill created successfully.", response));
    }

    @PutMapping("/api/subcontractor-bills/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<SubcontractorBillResponse>> update(
            @PathVariable Long id, @Valid @RequestBody SubcontractorBillRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Subcontractor bill updated successfully.", subcontractorBillService.update(id, request)));
    }

    @PatchMapping("/api/subcontractor-bills/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<SubcontractorBillResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Subcontractor bill approved.", subcontractorBillService.approve(id)));
    }
}
