package com.buildflow.material.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.material.dto.MaterialUsageRequest;
import com.buildflow.material.dto.MaterialUsageResponse;
import com.buildflow.material.service.MaterialUsageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MaterialUsageController {

    private final MaterialUsageService materialUsageService;

    public MaterialUsageController(MaterialUsageService materialUsageService) {
        this.materialUsageService = materialUsageService;
    }

    @GetMapping("/api/projects/{projectId}/materials/usage")
    public ResponseEntity<ApiResponse<List<MaterialUsageResponse>>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(materialUsageService.list(projectId)));
    }

    @PostMapping("/api/materials/{materialId}/usage")
    public ResponseEntity<ApiResponse<MaterialUsageResponse>> create(
            @PathVariable Long materialId, @Valid @RequestBody MaterialUsageRequest request) {
        MaterialUsageResponse response = materialUsageService.create(materialId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Usage recorded successfully.", response));
    }
}
