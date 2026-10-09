package com.buildflow.estimation.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.estimation.dto.BoqGenerationRuleRequest;
import com.buildflow.estimation.dto.BoqGenerationRuleResponse;
import com.buildflow.estimation.service.BoqGenerationRuleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BoqGenerationRuleController {

    private final BoqGenerationRuleService ruleService;

    public BoqGenerationRuleController(BoqGenerationRuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping("/api/boq-generation-rules")
    public ResponseEntity<ApiResponse<List<BoqGenerationRuleResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(ruleService.list()));
    }

    @PutMapping("/api/boq-generation-rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BoqGenerationRuleResponse>> update(
            @PathVariable Long id, @Valid @RequestBody BoqGenerationRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Generation rule saved.", ruleService.update(id, request)));
    }

    @DeleteMapping("/api/boq-generation-rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> revert(@PathVariable Long id) {
        ruleService.revertToPlatform(id);
        return ResponseEntity.ok(ApiResponse.success("Override removed; the platform rule applies again.", null));
    }
}
