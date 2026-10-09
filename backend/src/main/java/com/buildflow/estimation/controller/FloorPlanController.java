package com.buildflow.estimation.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.estimation.dto.HouseRequirementResponse;
import com.buildflow.estimation.dto.PlanModels.ColumnDto;
import com.buildflow.estimation.dto.PlanModels.FloorPlanResponse;
import com.buildflow.estimation.dto.PlanModels.LayoutRequest;
import com.buildflow.estimation.dto.PlanModels.PlansResponse;
import com.buildflow.estimation.service.FloorPlanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FloorPlanController {

    private final FloorPlanService floorPlanService;

    public FloorPlanController(FloorPlanService floorPlanService) {
        this.floorPlanService = floorPlanService;
    }

    @GetMapping("/api/house-requirements/{id}/plans")
    public ResponseEntity<ApiResponse<PlansResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(floorPlanService.get(id)));
    }

    @GetMapping("/api/house-requirements/{id}/plans/dxf")
    public ResponseEntity<byte[]> dxf(@PathVariable Long id) {
        byte[] body = floorPlanService.exportDxf(id).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"house-plan-" + id + ".dxf\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("application/dxf"))
                .body(body);
    }

    @PutMapping("/api/house-requirements/{id}/plans/{floorLevel}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<FloorPlanResponse>> save(
            @PathVariable Long id, @PathVariable int floorLevel, @Valid @RequestBody LayoutRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Floor plan saved.", floorPlanService.save(id, floorLevel, request)));
    }

    @PostMapping("/api/house-requirements/{id}/plans/suggest")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<PlansResponse>> suggest(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(floorPlanService.suggest(id)));
    }

    @PostMapping("/api/house-requirements/{id}/plans/columns/suggest")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<List<ColumnDto>>> suggestColumns(
            @PathVariable Long id, @Valid @RequestBody LayoutRequest request,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "grid") String style) {
        return ResponseEntity.ok(ApiResponse.success(
                floorPlanService.suggestColumns(id, request, "junctions".equalsIgnoreCase(style))));
    }

    @PostMapping("/api/house-requirements/{id}/plans/apply")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<HouseRequirementResponse>> apply(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Plan applied to the estimate. Regenerate the estimate to use it.", floorPlanService.applyToChecklist(id)));
    }
}
