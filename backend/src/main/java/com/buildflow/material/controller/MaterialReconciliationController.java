package com.buildflow.material.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.material.dto.MaterialReconciliationResponse;
import com.buildflow.material.service.MaterialReconciliationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MaterialReconciliationController {

    private final MaterialReconciliationService materialReconciliationService;

    public MaterialReconciliationController(MaterialReconciliationService materialReconciliationService) {
        this.materialReconciliationService = materialReconciliationService;
    }

    @GetMapping("/api/projects/{projectId}/materials/reconciliation")
    public ResponseEntity<ApiResponse<MaterialReconciliationResponse>> getReconciliation(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(materialReconciliationService.getReconciliation(projectId)));
    }
}
