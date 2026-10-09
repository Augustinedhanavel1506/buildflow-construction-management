package com.buildflow.subcontractor.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.subcontractor.dto.SubcontractorRequest;
import com.buildflow.subcontractor.dto.SubcontractorResponse;
import com.buildflow.subcontractor.service.SubcontractorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SubcontractorController {

    private final SubcontractorService subcontractorService;

    public SubcontractorController(SubcontractorService subcontractorService) {
        this.subcontractorService = subcontractorService;
    }

    @GetMapping("/api/subcontractors")
    public ResponseEntity<ApiResponse<List<SubcontractorResponse>>> list(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(ApiResponse.success(subcontractorService.list(includeInactive)));
    }

    @PostMapping("/api/subcontractors")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<SubcontractorResponse>> create(@Valid @RequestBody SubcontractorRequest request) {
        SubcontractorResponse response = subcontractorService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subcontractor added successfully.", response));
    }

    @PutMapping("/api/subcontractors/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<SubcontractorResponse>> update(
            @PathVariable Long id, @Valid @RequestBody SubcontractorRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Subcontractor updated successfully.", subcontractorService.update(id, request)));
    }
}
