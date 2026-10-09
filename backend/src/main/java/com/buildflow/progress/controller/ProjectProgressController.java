package com.buildflow.progress.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.progress.dto.ProgressStageRequest;
import com.buildflow.progress.dto.ProgressStageResponse;
import com.buildflow.progress.dto.ProgressUpdateRequest;
import com.buildflow.progress.service.ProjectProgressService;
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
public class ProjectProgressController {

    private final ProjectProgressService projectProgressService;

    public ProjectProgressController(ProjectProgressService projectProgressService) {
        this.projectProgressService = projectProgressService;
    }

    @GetMapping("/api/projects/{projectId}/progress")
    public ResponseEntity<ApiResponse<List<ProgressStageResponse>>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(projectProgressService.list(projectId)));
    }

    @PostMapping("/api/projects/{projectId}/progress")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<ProgressStageResponse>> create(
            @PathVariable Long projectId, @Valid @RequestBody ProgressStageRequest request) {
        ProgressStageResponse response = projectProgressService.create(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Progress stage added successfully.", response));
    }

    @PatchMapping("/api/progress/{id}")
    public ResponseEntity<ApiResponse<ProgressStageResponse>> updatePercent(
            @PathVariable Long id, @Valid @RequestBody ProgressUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Progress updated successfully.",
                projectProgressService.updatePercent(id, request)));
    }
}
