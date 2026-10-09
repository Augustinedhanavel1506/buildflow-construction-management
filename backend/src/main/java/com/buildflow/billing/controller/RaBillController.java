package com.buildflow.billing.controller;

import com.buildflow.billing.dto.BillingSummaryResponse;
import com.buildflow.billing.dto.CertifyBillRequest;
import com.buildflow.billing.dto.RaBillRequest;
import com.buildflow.billing.dto.RaBillResponse;
import com.buildflow.billing.service.RaBillService;
import com.buildflow.common.dto.ApiResponse;
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
public class RaBillController {

    private final RaBillService raBillService;

    public RaBillController(RaBillService raBillService) {
        this.raBillService = raBillService;
    }

    @GetMapping("/api/projects/{projectId}/billing/ra-bills")
    public ResponseEntity<ApiResponse<List<RaBillResponse>>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(raBillService.list(projectId)));
    }

    @PostMapping("/api/projects/{projectId}/billing/ra-bills")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<RaBillResponse>> create(
            @PathVariable Long projectId, @Valid @RequestBody RaBillRequest request) {
        RaBillResponse response = raBillService.create(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("RA bill created successfully.", response));
    }

    @PutMapping("/api/billing/ra-bills/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<RaBillResponse>> update(
            @PathVariable Long id, @Valid @RequestBody RaBillRequest request) {
        return ResponseEntity.ok(ApiResponse.success("RA bill updated successfully.", raBillService.update(id, request)));
    }

    @PatchMapping("/api/billing/ra-bills/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<RaBillResponse>> submit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("RA bill submitted successfully.", raBillService.submit(id)));
    }

    @PatchMapping("/api/billing/ra-bills/{id}/certify")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<RaBillResponse>> certify(
            @PathVariable Long id, @Valid @RequestBody CertifyBillRequest request) {
        return ResponseEntity.ok(ApiResponse.success("RA bill certified successfully.", raBillService.certify(id, request)));
    }

    @GetMapping("/api/projects/{projectId}/billing/summary")
    public ResponseEntity<ApiResponse<BillingSummaryResponse>> getSummary(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(raBillService.getSummary(projectId)));
    }
}
