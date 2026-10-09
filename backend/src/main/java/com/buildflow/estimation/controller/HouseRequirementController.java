package com.buildflow.estimation.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.estimation.dto.BoqGenerationResultResponse;
import com.buildflow.estimation.dto.HouseRequirementRequest;
import com.buildflow.estimation.dto.HouseRequirementResponse;
import com.buildflow.estimation.service.BoqGenerationService;
import com.buildflow.estimation.service.HouseRequirementService;
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
public class HouseRequirementController {

    private final HouseRequirementService houseRequirementService;
    private final BoqGenerationService boqGenerationService;

    public HouseRequirementController(HouseRequirementService houseRequirementService,
                                       BoqGenerationService boqGenerationService) {
        this.houseRequirementService = houseRequirementService;
        this.boqGenerationService = boqGenerationService;
    }

    @GetMapping("/api/house-requirements")
    public ResponseEntity<ApiResponse<List<HouseRequirementResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(houseRequirementService.list()));
    }

    @GetMapping("/api/house-requirements/{id}")
    public ResponseEntity<ApiResponse<HouseRequirementResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(houseRequirementService.get(id)));
    }

    @PostMapping("/api/house-requirements")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<HouseRequirementResponse>> create(@Valid @RequestBody HouseRequirementRequest request) {
        HouseRequirementResponse response = houseRequirementService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("House requirement saved successfully.", response));
    }

    @PutMapping("/api/house-requirements/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<HouseRequirementResponse>> update(
            @PathVariable Long id, @Valid @RequestBody HouseRequirementRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "House requirement updated successfully.", houseRequirementService.update(id, request)));
    }

    @PostMapping("/api/house-requirements/{id}/generate-boq")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<BoqGenerationResultResponse>> generateBoq(@PathVariable Long id) {
        BoqGenerationResultResponse response = boqGenerationService.generate(id);
        return ResponseEntity.ok(ApiResponse.success("Preliminary BOQ generated successfully.", response));
    }
}
