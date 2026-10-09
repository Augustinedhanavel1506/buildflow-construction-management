package com.buildflow.dailyreport.dto;

import com.buildflow.dailyreport.entity.DailyReport;

import java.time.LocalDate;

public record DailyReportResponse(
        Long id,
        Long projectId,
        LocalDate reportDate,
        Integer workersPresent,
        String workCompleted,
        String materialsUsed,
        String issues,
        String notes,
        Long submittedById,
        String submittedByName
) {
    public static DailyReportResponse from(DailyReport report) {
        return new DailyReportResponse(
                report.getId(),
                report.getProject().getId(),
                report.getReportDate(),
                report.getWorkersPresent(),
                report.getWorkCompleted(),
                report.getMaterialsUsed(),
                report.getIssues(),
                report.getNotes(),
                report.getSubmittedBy().getId(),
                report.getSubmittedBy().getFullName()
        );
    }
}
