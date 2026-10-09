package com.buildflow.billing.controller;

import com.buildflow.billing.dto.GstInvoiceRequest;
import com.buildflow.billing.dto.GstInvoiceResponse;
import com.buildflow.billing.service.GstInvoiceService;
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
public class GstInvoiceController {

    private final GstInvoiceService gstInvoiceService;

    public GstInvoiceController(GstInvoiceService gstInvoiceService) {
        this.gstInvoiceService = gstInvoiceService;
    }

    @PostMapping("/api/billing/ra-bills/{raBillId}/gst-invoice")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<GstInvoiceResponse>> create(
            @PathVariable Long raBillId, @Valid @RequestBody GstInvoiceRequest request) {
        GstInvoiceResponse response = gstInvoiceService.create(raBillId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("GST invoice issued successfully.", response));
    }

    @GetMapping("/api/billing/ra-bills/{raBillId}/gst-invoice")
    public ResponseEntity<ApiResponse<GstInvoiceResponse>> getForBill(@PathVariable Long raBillId) {
        return ResponseEntity.ok(ApiResponse.success(gstInvoiceService.getForBill(raBillId)));
    }

    @GetMapping("/api/billing/gst-invoices/{id}")
    public ResponseEntity<ApiResponse<GstInvoiceResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(gstInvoiceService.get(id)));
    }

    @GetMapping("/api/billing/gst-invoices")
    public ResponseEntity<ApiResponse<List<GstInvoiceResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(gstInvoiceService.list()));
    }
}
