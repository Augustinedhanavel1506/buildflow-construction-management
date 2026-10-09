package com.buildflow.boq.controller;

import com.buildflow.boq.dto.BoqItemRequest;
import com.buildflow.boq.dto.BoqItemResponse;
import com.buildflow.boq.dto.BoqValidationRequest;
import com.buildflow.boq.service.BoqItemService;
import com.buildflow.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BoqItemController {

    private final BoqItemService boqItemService;

    public BoqItemController(BoqItemService boqItemService) {
        this.boqItemService = boqItemService;
    }

    @GetMapping("/api/projects/{projectId}/boq")
    public ResponseEntity<ApiResponse<List<BoqItemResponse>>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(boqItemService.list(projectId)));
    }

    @PostMapping("/api/projects/{projectId}/boq/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<BoqItemResponse>> create(
            @PathVariable Long projectId, @Valid @RequestBody BoqItemRequest request) {
        BoqItemResponse response = boqItemService.create(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("BOQ item added successfully.", response));
    }

    @PutMapping("/api/boq/items/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<BoqItemResponse>> update(
            @PathVariable Long id, @Valid @RequestBody BoqItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("BOQ item updated successfully.", boqItemService.update(id, request)));
    }

    @PostMapping("/api/boq/items/{id}/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<BoqItemResponse>> validate(
            @PathVariable Long id, @Valid @RequestBody BoqValidationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("BOQ item validated.", boqItemService.validate(id, request)));
    }

    @PostMapping("/api/projects/{projectId}/boq/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<List<BoqItemResponse>>> validateAll(
            @PathVariable Long projectId, @Valid @RequestBody BoqValidationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Preliminary BOQ lines validated.", boqItemService.validateAll(projectId, request)));
    }

    @DeleteMapping("/api/boq/items/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        boqItemService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("BOQ item deleted successfully.", null));
    }
}
