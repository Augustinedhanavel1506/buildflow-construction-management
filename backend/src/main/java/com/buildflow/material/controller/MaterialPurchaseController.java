package com.buildflow.material.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.material.dto.MaterialPurchaseRequest;
import com.buildflow.material.dto.MaterialPurchaseResponse;
import com.buildflow.material.service.MaterialPurchaseService;
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
public class MaterialPurchaseController {

    private final MaterialPurchaseService materialPurchaseService;

    public MaterialPurchaseController(MaterialPurchaseService materialPurchaseService) {
        this.materialPurchaseService = materialPurchaseService;
    }

    @GetMapping("/api/projects/{projectId}/materials/purchases")
    public ResponseEntity<ApiResponse<List<MaterialPurchaseResponse>>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(materialPurchaseService.list(projectId)));
    }

    @PostMapping("/api/materials/{materialId}/purchases")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<MaterialPurchaseResponse>> create(
            @PathVariable Long materialId, @Valid @RequestBody MaterialPurchaseRequest request) {
        MaterialPurchaseResponse response = materialPurchaseService.create(materialId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Purchase recorded successfully.", response));
    }
}
