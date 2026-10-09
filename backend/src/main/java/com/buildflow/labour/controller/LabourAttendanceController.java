package com.buildflow.labour.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.labour.dto.AttendanceEntryResponse;
import com.buildflow.labour.dto.AttendanceSubmitRequest;
import com.buildflow.labour.dto.LabourCostResponse;
import com.buildflow.labour.service.LabourAttendanceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
public class LabourAttendanceController {

    private final LabourAttendanceService labourAttendanceService;

    public LabourAttendanceController(LabourAttendanceService labourAttendanceService) {
        this.labourAttendanceService = labourAttendanceService;
    }

    @GetMapping("/api/projects/{projectId}/labour/attendance")
    public ResponseEntity<ApiResponse<List<AttendanceEntryResponse>>> getForDate(
            @PathVariable Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(labourAttendanceService.getForDate(projectId, date)));
    }

    @PostMapping("/api/projects/{projectId}/labour/attendance")
    public ResponseEntity<ApiResponse<List<AttendanceEntryResponse>>> submit(
            @PathVariable Long projectId, @Valid @RequestBody AttendanceSubmitRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Attendance submitted successfully.",
                labourAttendanceService.submit(projectId, request)));
    }

    @GetMapping("/api/projects/{projectId}/labour/cost")
    public ResponseEntity<ApiResponse<LabourCostResponse>> getCostSummary(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(labourAttendanceService.getCostSummary(projectId)));
    }
}
