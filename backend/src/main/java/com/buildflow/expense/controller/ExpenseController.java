package com.buildflow.expense.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.expense.dto.ExpenseRequest;
import com.buildflow.expense.dto.ExpenseResponse;
import com.buildflow.expense.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping("/api/projects/{projectId}/expenses")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.list(projectId)));
    }

    @PostMapping("/api/projects/{projectId}/expenses")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<ExpenseResponse>> create(
            @PathVariable Long projectId, @Valid @RequestBody ExpenseRequest request) {
        ExpenseResponse response = expenseService.create(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Expense added successfully.", response));
    }

    @GetMapping("/api/expenses/{id}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.get(id)));
    }

    @PutMapping("/api/expenses/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<ExpenseResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Expense updated successfully.", expenseService.update(id, request)));
    }

    @DeleteMapping("/api/expenses/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        expenseService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Expense deleted successfully.", null));
    }
}
