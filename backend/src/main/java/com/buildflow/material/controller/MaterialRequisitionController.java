package com.buildflow.material.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.material.dto.MaterialRequisitionRequest;
import com.buildflow.material.dto.MaterialRequisitionResponse;
import com.buildflow.material.service.MaterialRequisitionService;
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
public class MaterialRequisitionController {

    private final MaterialRequisitionService materialRequisitionService;

    public MaterialRequisitionController(MaterialRequisitionService materialRequisitionService) {
        this.materialRequisitionService = materialRequisitionService;
    }

    @GetMapping("/api/projects/{projectId}/materials/requests")
    public ResponseEntity<ApiResponse<List<MaterialRequisitionResponse>>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(materialRequisitionService.list(projectId)));
    }

    @PostMapping("/api/projects/{projectId}/materials/requests")
    public ResponseEntity<ApiResponse<MaterialRequisitionResponse>> create(
            @PathVariable Long projectId, @Valid @RequestBody MaterialRequisitionRequest request) {
        MaterialRequisitionResponse response = materialRequisitionService.create(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Material request submitted successfully.", response));
    }

    @PatchMapping("/api/materials/requests/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<MaterialRequisitionResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Request approved.", materialRequisitionService.approve(id)));
    }

    @PatchMapping("/api/materials/requests/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<MaterialRequisitionResponse>> reject(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Request rejected.", materialRequisitionService.reject(id)));
    }
}
