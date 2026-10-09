package com.buildflow.subcontractor.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.subcontractor.dto.WorkOrderRequest;
import com.buildflow.subcontractor.dto.WorkOrderResponse;
import com.buildflow.subcontractor.service.SubcontractWorkOrderService;
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
public class SubcontractWorkOrderController {

    private final SubcontractWorkOrderService workOrderService;

    public SubcontractWorkOrderController(SubcontractWorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @GetMapping("/api/projects/{projectId}/subcontract-work-orders")
    public ResponseEntity<ApiResponse<List<WorkOrderResponse>>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.list(projectId)));
    }

    @PostMapping("/api/projects/{projectId}/subcontract-work-orders")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> create(
            @PathVariable Long projectId, @Valid @RequestBody WorkOrderRequest request) {
        WorkOrderResponse response = workOrderService.create(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Work order created successfully.", response));
    }
}
