package com.buildflow.estimation.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.estimation.dto.WhatIfModels.WhatIfRequest;
import com.buildflow.estimation.dto.WhatIfModels.WhatIfResponse;
import com.buildflow.estimation.service.WhatIfService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WhatIfController {

    private final WhatIfService whatIfService;

    public WhatIfController(WhatIfService whatIfService) {
        this.whatIfService = whatIfService;
    }

    @PostMapping("/api/house-requirements/{id}/what-if")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<WhatIfResponse>> compare(
            @PathVariable Long id, @Valid @RequestBody WhatIfRequest request) {
        return ResponseEntity.ok(ApiResponse.success(whatIfService.compare(id, request)));
    }
}
