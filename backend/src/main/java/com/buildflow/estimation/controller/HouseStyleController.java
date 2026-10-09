package com.buildflow.estimation.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.estimation.service.HouseStyleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
public class HouseStyleController {

    private final HouseStyleService houseStyleService;

    public HouseStyleController(HouseStyleService houseStyleService) {
        this.houseStyleService = houseStyleService;
    }

    @GetMapping("/api/house-requirements/{id}/house-style")
    public ResponseEntity<ApiResponse<JsonNode>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(houseStyleService.get(id)));
    }

    @PutMapping("/api/house-requirements/{id}/house-style")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<JsonNode>> save(@PathVariable Long id, @RequestBody JsonNode style) {
        return ResponseEntity.ok(ApiResponse.success("House style saved.", houseStyleService.save(id, style)));
    }
}
