package com.buildflow.business.controller;

import com.buildflow.business.dto.BusinessRequest;
import com.buildflow.business.dto.BusinessResponse;
import com.buildflow.business.service.BusinessService;
import com.buildflow.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @GetMapping("/api/business")
    public ResponseEntity<ApiResponse<BusinessResponse>> get() {
        return ResponseEntity.ok(ApiResponse.success(businessService.get()));
    }

    @PutMapping("/api/business")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BusinessResponse>> update(@Valid @RequestBody BusinessRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Business profile updated successfully.", businessService.update(request)));
    }
}
