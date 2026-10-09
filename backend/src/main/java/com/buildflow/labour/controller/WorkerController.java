package com.buildflow.labour.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.labour.dto.WorkerRequest;
import com.buildflow.labour.dto.WorkerResponse;
import com.buildflow.labour.service.WorkerService;
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
public class WorkerController {

    private final WorkerService workerService;

    public WorkerController(WorkerService workerService) {
        this.workerService = workerService;
    }

    @GetMapping("/api/projects/{projectId}/labour/workers")
    public ResponseEntity<ApiResponse<List<WorkerResponse>>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(workerService.list(projectId)));
    }

    @PostMapping("/api/projects/{projectId}/labour/workers")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<WorkerResponse>> create(
            @PathVariable Long projectId, @Valid @RequestBody WorkerRequest request) {
        WorkerResponse response = workerService.create(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Worker added successfully.", response));
    }

    @PutMapping("/api/labour/workers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<WorkerResponse>> update(
            @PathVariable Long id, @Valid @RequestBody WorkerRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Worker updated successfully.", workerService.update(id, request)));
    }
}
