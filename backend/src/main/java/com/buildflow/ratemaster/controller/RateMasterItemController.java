package com.buildflow.ratemaster.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.ratemaster.dto.RateMasterItemRequest;
import com.buildflow.ratemaster.dto.RateMasterItemResponse;
import com.buildflow.ratemaster.service.RateMasterItemService;
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
public class RateMasterItemController {

    private final RateMasterItemService rateMasterItemService;

    public RateMasterItemController(RateMasterItemService rateMasterItemService) {
        this.rateMasterItemService = rateMasterItemService;
    }

    @GetMapping("/api/rate-master")
    public ResponseEntity<ApiResponse<List<RateMasterItemResponse>>> list(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(ApiResponse.success(rateMasterItemService.list(includeInactive)));
    }

    @PostMapping("/api/rate-master")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<RateMasterItemResponse>> create(@Valid @RequestBody RateMasterItemRequest request) {
        RateMasterItemResponse response = rateMasterItemService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Rate master item added successfully.", response));
    }

    @PostMapping("/api/rate-master/starter")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<RateMasterItemService.StarterRatesResult>> addStarterRates() {
        RateMasterItemService.StarterRatesResult result = rateMasterItemService.addStarterRates();
        String message = result.added() == 0
                ? "You already have every starter item."
                : result.added() + " starter rates added. They are illustrative, so replace them with your local rates.";
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

    public record CopyDistrictRequest(String fromDistrict, String toDistrict, java.math.BigDecimal adjustPercent) {
    }

    @PostMapping("/api/rate-master/copy-district")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<RateMasterItemService.CopyDistrictResult>> copyDistrict(@RequestBody CopyDistrictRequest request) {
        RateMasterItemService.CopyDistrictResult result =
                rateMasterItemService.copyToDistrict(request.fromDistrict(), request.toDistrict(), request.adjustPercent());
        return ResponseEntity.ok(ApiResponse.success(result.added() + " rates copied to " + request.toDistrict().trim() + ".", result));
    }

    @PutMapping("/api/rate-master/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<RateMasterItemResponse>> update(
            @PathVariable Long id, @Valid @RequestBody RateMasterItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Rate master item updated successfully.", rateMasterItemService.update(id, request)));
    }
}
