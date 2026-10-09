package com.buildflow.dealer.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.dealer.dto.DealerRequest;
import com.buildflow.dealer.dto.DealerResponse;
import com.buildflow.dealer.service.DealerService;
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
public class DealerController {

    private final DealerService dealerService;

    public DealerController(DealerService dealerService) {
        this.dealerService = dealerService;
    }

    @GetMapping("/api/dealers")
    public ResponseEntity<ApiResponse<List<DealerResponse>>> list(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String item,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(ApiResponse.success(dealerService.list(district, item, includeInactive)));
    }

    @GetMapping("/api/dealers/{id}")
    public ResponseEntity<ApiResponse<DealerResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(dealerService.get(id)));
    }

    @PostMapping("/api/dealers")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<DealerResponse>> create(@Valid @RequestBody DealerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Dealer added successfully.", dealerService.create(request)));
    }

    @PutMapping("/api/dealers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<DealerResponse>> update(
            @PathVariable Long id, @Valid @RequestBody DealerRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Dealer updated successfully.", dealerService.update(id, request)));
    }
}
