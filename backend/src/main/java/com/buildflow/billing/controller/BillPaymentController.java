package com.buildflow.billing.controller;

import com.buildflow.billing.dto.BillPaymentRequest;
import com.buildflow.billing.dto.BillPaymentResponse;
import com.buildflow.billing.service.BillPaymentService;
import com.buildflow.common.dto.ApiResponse;
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
public class BillPaymentController {

    private final BillPaymentService billPaymentService;

    public BillPaymentController(BillPaymentService billPaymentService) {
        this.billPaymentService = billPaymentService;
    }

    @GetMapping("/api/billing/ra-bills/{raBillId}/payments")
    public ResponseEntity<ApiResponse<List<BillPaymentResponse>>> list(@PathVariable Long raBillId) {
        return ResponseEntity.ok(ApiResponse.success(billPaymentService.list(raBillId)));
    }

    @PostMapping("/api/billing/ra-bills/{raBillId}/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<BillPaymentResponse>> create(
            @PathVariable Long raBillId, @Valid @RequestBody BillPaymentRequest request) {
        BillPaymentResponse response = billPaymentService.create(raBillId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment recorded successfully.", response));
    }
}
