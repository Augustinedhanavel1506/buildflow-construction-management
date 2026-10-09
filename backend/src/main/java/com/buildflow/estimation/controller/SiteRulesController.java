package com.buildflow.estimation.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.estimation.dto.SiteModels.SiteCheckResponse;
import com.buildflow.estimation.dto.SiteModels.SiteRulesRequest;
import com.buildflow.estimation.service.SiteRulesService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SiteRulesController {

    private final SiteRulesService siteRulesService;

    public SiteRulesController(SiteRulesService siteRulesService) {
        this.siteRulesService = siteRulesService;
    }

    @GetMapping("/api/house-requirements/{id}/site-rules")
    public ResponseEntity<ApiResponse<SiteCheckResponse>> check(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(siteRulesService.check(id)));
    }

    @PutMapping("/api/house-requirements/{id}/site-rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<SiteCheckResponse>> save(
            @PathVariable Long id, @Valid @RequestBody SiteRulesRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Site rules saved.", siteRulesService.save(id, request)));
    }
}
