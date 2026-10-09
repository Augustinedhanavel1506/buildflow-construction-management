package com.buildflow.calculator.controller;

import com.buildflow.calculator.dto.CalculatorModels.ConcreteRequest;
import com.buildflow.calculator.dto.CalculatorModels.ConcreteResult;
import com.buildflow.calculator.dto.CalculatorModels.MasonryRequest;
import com.buildflow.calculator.dto.CalculatorModels.MasonryResult;
import com.buildflow.calculator.dto.CalculatorModels.SteelRequest;
import com.buildflow.calculator.dto.CalculatorModels.SteelResult;
import com.buildflow.calculator.service.CalculatorService;
import com.buildflow.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CalculatorController {

    private final CalculatorService calculatorService;

    public CalculatorController(CalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @PostMapping("/api/calculators/concrete")
    public ResponseEntity<ApiResponse<ConcreteResult>> concrete(@Valid @RequestBody ConcreteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(calculatorService.concrete(request)));
    }

    @PostMapping("/api/calculators/masonry")
    public ResponseEntity<ApiResponse<MasonryResult>> masonry(@Valid @RequestBody MasonryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(calculatorService.masonry(request)));
    }

    @PostMapping("/api/calculators/steel")
    public ResponseEntity<ApiResponse<SteelResult>> steel(@Valid @RequestBody SteelRequest request) {
        return ResponseEntity.ok(ApiResponse.success(calculatorService.steel(request)));
    }
}
