package com.buildflow.report.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.report.dto.BoqVarianceReportResponse;
import com.buildflow.report.dto.ProfitabilityReportResponse;
import com.buildflow.report.dto.ProjectCostReportResponse;
import com.buildflow.report.service.ReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/api/projects/{projectId}/reports/cost-summary")
    public ResponseEntity<ApiResponse<ProjectCostReportResponse>> getCostSummary(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getProjectCostReport(projectId)));
    }

    @GetMapping("/api/projects/{projectId}/reports/boq-variance")
    public ResponseEntity<ApiResponse<BoqVarianceReportResponse>> getBoqVariance(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getBoqVarianceReport(projectId)));
    }

    @GetMapping("/api/projects/{projectId}/reports/boq-variance/export")
    public ResponseEntity<String> exportBoqVariance(@PathVariable Long projectId) {
        return csvResponse(reportService.exportBoqVarianceCsv(projectId), "boq-variance-" + projectId + ".csv");
    }

    @GetMapping("/api/projects/{projectId}/reports/expenses/export")
    public ResponseEntity<String> exportExpenses(@PathVariable Long projectId) {
        return csvResponse(reportService.exportExpensesCsv(projectId), "expenses-" + projectId + ".csv");
    }

    @GetMapping("/api/reports/profitability")
    public ResponseEntity<ApiResponse<ProfitabilityReportResponse>> getProfitability() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getProfitabilityReport()));
    }

    @GetMapping("/api/reports/profitability/export")
    public ResponseEntity<String> exportProfitability() {
        return csvResponse(reportService.exportProfitabilityCsv(), "profitability-report.csv");
    }

    private ResponseEntity<String> csvResponse(String csv, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(csv);
    }
}
