package com.buildflow.dailyreport.service;

import com.buildflow.auth.entity.Role;
import com.buildflow.auth.entity.User;
import com.buildflow.common.exception.ForbiddenException;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.dailyreport.dto.DailyReportRequest;
import com.buildflow.dailyreport.dto.DailyReportResponse;
import com.buildflow.dailyreport.entity.DailyReport;
import com.buildflow.dailyreport.repository.DailyReportRepository;
import com.buildflow.notification.entity.NotificationType;
import com.buildflow.notification.service.NotificationService;
import com.buildflow.project.entity.Project;
import com.buildflow.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DailyReportService {

    private final DailyReportRepository dailyReportRepository;
    private final ProjectRepository projectRepository;
    private final NotificationService notificationService;
    private final CurrentUserProvider currentUserProvider;

    public DailyReportService(DailyReportRepository dailyReportRepository,
                               ProjectRepository projectRepository,
                               NotificationService notificationService,
                               CurrentUserProvider currentUserProvider) {
        this.dailyReportRepository = dailyReportRepository;
        this.projectRepository = projectRepository;
        this.notificationService = notificationService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<DailyReportResponse> list(Long projectId) {
        Project project = findOwnedProject(projectId);
        return dailyReportRepository.findByProjectIdOrderByReportDateDesc(project.getId()).stream()
                .map(DailyReportResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DailyReportResponse get(Long id) {
        return DailyReportResponse.from(findOwnedReport(id));
    }

    @Transactional
    public DailyReportResponse create(Long projectId, DailyReportRequest request) {
        Project project = findOwnedProject(projectId);

        DailyReport report = new DailyReport();
        report.setProject(project);
        report.setSubmittedBy(currentUserProvider.getCurrentUser());
        applyRequest(report, request);

        DailyReport saved = dailyReportRepository.save(report);

        String message = "%s submitted a daily report for %s".formatted(saved.getSubmittedBy().getFullName(), project.getName());
        notificationService.notifyRole(project.getBusiness(), Role.ADMIN, NotificationType.DAILY_REPORT,
                "Daily report submitted", message, "DAILY_REPORT", saved.getId(), project.getId());
        notificationService.notifyRole(project.getBusiness(), Role.PROJECT_MANAGER, NotificationType.DAILY_REPORT,
                "Daily report submitted", message, "DAILY_REPORT", saved.getId(), project.getId());

        return DailyReportResponse.from(saved);
    }

    @Transactional
    public DailyReportResponse update(Long id, DailyReportRequest request) {
        DailyReport report = findOwnedReport(id);
        User currentUser = currentUserProvider.getCurrentUser();

        boolean isAuthor = report.getSubmittedBy().getId().equals(currentUser.getId());
        boolean isManager = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.PROJECT_MANAGER;
        if (!isAuthor && !isManager) {
            throw new ForbiddenException("You don't have permission to perform this action.");
        }

        applyRequest(report, request);
        return DailyReportResponse.from(dailyReportRepository.save(report));
    }

    private void applyRequest(DailyReport report, DailyReportRequest request) {
        report.setReportDate(request.reportDate());
        report.setWorkersPresent(request.workersPresent());
        report.setWorkCompleted(request.workCompleted());
        report.setMaterialsUsed(request.materialsUsed());
        report.setIssues(request.issues());
        report.setNotes(request.notes());
    }

    private Project findOwnedProject(Long projectId) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return projectRepository.findByIdAndBusinessId(projectId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
    }

    private DailyReport findOwnedReport(Long id) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return dailyReportRepository.findByIdAndProjectBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Daily report not found."));
    }
}
