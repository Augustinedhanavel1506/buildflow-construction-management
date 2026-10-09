package com.buildflow.material.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.material.dto.MaterialRequest;
import com.buildflow.material.dto.MaterialResponse;
import com.buildflow.material.service.MaterialService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping("/api/projects/{projectId}/materials")
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(materialService.list(projectId)));
    }

    @PostMapping("/api/projects/{projectId}/materials")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<MaterialResponse>> create(
            @PathVariable Long projectId, @Valid @RequestBody MaterialRequest request) {
        MaterialResponse response = materialService.create(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Material added successfully.", response));
    }

    @PutMapping("/api/materials/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<MaterialResponse>> update(
            @PathVariable Long id, @Valid @RequestBody MaterialRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Material updated successfully.", materialService.update(id, request)));
    }
}
