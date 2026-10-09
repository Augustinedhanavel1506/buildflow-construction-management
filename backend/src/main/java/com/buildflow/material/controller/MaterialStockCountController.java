package com.buildflow.material.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.material.dto.MaterialStockCountRequest;
import com.buildflow.material.dto.MaterialStockCountResponse;
import com.buildflow.material.service.MaterialStockCountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MaterialStockCountController {

    private final MaterialStockCountService materialStockCountService;

    public MaterialStockCountController(MaterialStockCountService materialStockCountService) {
        this.materialStockCountService = materialStockCountService;
    }

    @GetMapping("/api/materials/{materialId}/stock-counts")
    public ResponseEntity<ApiResponse<List<MaterialStockCountResponse>>> list(@PathVariable Long materialId) {
        return ResponseEntity.ok(ApiResponse.success(materialStockCountService.list(materialId)));
    }

    @PostMapping("/api/materials/{materialId}/stock-counts")
    public ResponseEntity<ApiResponse<MaterialStockCountResponse>> create(
            @PathVariable Long materialId, @Valid @RequestBody MaterialStockCountRequest request) {
        MaterialStockCountResponse response = materialStockCountService.create(materialId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Stock count recorded successfully.", response));
    }
}
