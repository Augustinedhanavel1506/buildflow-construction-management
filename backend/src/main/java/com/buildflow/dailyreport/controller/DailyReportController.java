package com.buildflow.dailyreport.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.dailyreport.dto.DailyReportRequest;
import com.buildflow.dailyreport.dto.DailyReportResponse;
import com.buildflow.dailyreport.service.DailyReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DailyReportController {

    private final DailyReportService dailyReportService;

    public DailyReportController(DailyReportService dailyReportService) {
        this.dailyReportService = dailyReportService;
    }

    @GetMapping("/api/projects/{projectId}/daily-reports")
    public ResponseEntity<ApiResponse<List<DailyReportResponse>>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(dailyReportService.list(projectId)));
    }

    @PostMapping("/api/projects/{projectId}/daily-reports")
    public ResponseEntity<ApiResponse<DailyReportResponse>> create(
            @PathVariable Long projectId, @Valid @RequestBody DailyReportRequest request) {
        DailyReportResponse response = dailyReportService.create(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Daily report submitted successfully.", response));
    }

    @GetMapping("/api/daily-reports/{id}")
    public ResponseEntity<ApiResponse<DailyReportResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(dailyReportService.get(id)));
    }

    @PutMapping("/api/daily-reports/{id}")
    public ResponseEntity<ApiResponse<DailyReportResponse>> update(
            @PathVariable Long id, @Valid @RequestBody DailyReportRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Daily report updated successfully.", dailyReportService.update(id, request)));
    }
}
