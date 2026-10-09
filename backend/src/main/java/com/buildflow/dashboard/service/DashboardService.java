package com.buildflow.dashboard.service;

import com.buildflow.billing.dto.BillingSummaryResponse;
import com.buildflow.billing.service.RaBillService;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.dailyreport.entity.DailyReport;
import com.buildflow.dailyreport.repository.DailyReportRepository;
import com.buildflow.dashboard.dto.DashboardSummaryResponse;
import com.buildflow.dashboard.dto.DashboardSummaryResponse.ActivityItem;
import com.buildflow.dashboard.dto.DashboardSummaryResponse.BillingSummary;
import com.buildflow.dashboard.dto.DashboardSummaryResponse.CategoryAmount;
import com.buildflow.dashboard.dto.DashboardSummaryResponse.FinancialSummary;
import com.buildflow.dashboard.dto.DashboardSummaryResponse.ProjectCounts;
import com.buildflow.expense.entity.Expense;
import com.buildflow.expense.entity.ExpenseCategory;
import com.buildflow.expense.repository.ExpenseRepository;
import com.buildflow.material.service.MaterialReconciliationService;
import com.buildflow.progress.repository.ProjectProgressStageRepository;
import com.buildflow.project.entity.Project;
import com.buildflow.project.entity.ProjectStatus;
import com.buildflow.project.repository.ProjectRepository;
import com.buildflow.variation.service.VariationOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class DashboardService {

    private static final Set<ProjectStatus> OPEN_STATUSES =
            Set.of(ProjectStatus.PLANNING, ProjectStatus.ACTIVE, ProjectStatus.ON_HOLD);

    private final ProjectRepository projectRepository;
    private final ExpenseRepository expenseRepository;
    private final DailyReportRepository dailyReportRepository;
    private final ProjectProgressStageRepository progressStageRepository;
    private final VariationOrderService variationOrderService;
    private final RaBillService raBillService;
    private final MaterialReconciliationService materialReconciliationService;
    private final CurrentUserProvider currentUserProvider;

    public DashboardService(ProjectRepository projectRepository,
                             ExpenseRepository expenseRepository,
                             DailyReportRepository dailyReportRepository,
                             ProjectProgressStageRepository progressStageRepository,
                             VariationOrderService variationOrderService,
                             RaBillService raBillService,
                             MaterialReconciliationService materialReconciliationService,
                             CurrentUserProvider currentUserProvider) {
        this.projectRepository = projectRepository;
        this.expenseRepository = expenseRepository;
        this.dailyReportRepository = dailyReportRepository;
        this.progressStageRepository = progressStageRepository;
        this.variationOrderService = variationOrderService;
        this.raBillService = raBillService;
        this.materialReconciliationService = materialReconciliationService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        List<Project> projects = projectRepository.findByBusinessIdAndArchivedFalse(businessId);
        LocalDate today = LocalDate.now();

        long activeCount = projects.stream().filter(p -> p.getStatus() == ProjectStatus.ACTIVE).count();
        long completedCount = projects.stream().filter(p -> p.getStatus() == ProjectStatus.COMPLETED).count();
        long delayedCount = projects.stream()
                .filter(p -> OPEN_STATUSES.contains(p.getStatus()))
                .filter(p -> p.getExpectedEndDate() != null && p.getExpectedEndDate().isBefore(today))
                .count();

        ProjectCounts projectCounts = new ProjectCounts(projects.size(), activeCount, delayedCount, completedCount);

        BigDecimal rawContractValue = projects.stream()
                .map(Project::getContractValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalContractValue = rawContractValue.add(variationOrderService.getNetApprovedAmountForBusiness(businessId));
        BigDecimal totalEstimatedCost = projects.stream()
                .map(Project::getEstimatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalActualCost = expenseRepository.sumAmountByBusinessId(businessId);

        int budgetUtilizationPercent = totalContractValue.signum() > 0
                ? totalActualCost.multiply(BigDecimal.valueOf(100))
                        .divide(totalContractValue, 0, RoundingMode.HALF_UP)
                        .intValue()
                : 0;

        FinancialSummary financialSummary = new FinancialSummary(
                totalContractValue, totalEstimatedCost, totalActualCost, budgetUtilizationPercent);

        double overallProgress = projects.isEmpty()
                ? 0
                : projects.stream()
                        .mapToDouble(p -> progressStageRepository.averagePercentCompleteByProjectId(p.getId()))
                        .average()
                        .orElse(0);

        List<CategoryAmount> costBreakdown = expenseRepository.findCategoryBreakdownByBusinessId(businessId).stream()
                .map(row -> new CategoryAmount(((ExpenseCategory) row[0]).name(), (BigDecimal) row[1]))
                .toList();

        List<ActivityItem> recentActivity = buildRecentActivity(businessId);

        BillingSummaryResponse billing = raBillService.getSummaryForBusiness(businessId);
        BillingSummary billingSummary = new BillingSummary(
                billing.totalBilled(), billing.totalReceived(), billing.totalOutstanding(), billing.totalRetentionHeld());

        BigDecimal estimatedMaterialVarianceValue =
                materialReconciliationService.getTotalEstimatedVarianceValueForBusiness(businessId);

        return new DashboardSummaryResponse(
                projectCounts, financialSummary, billingSummary, estimatedMaterialVarianceValue,
                (int) Math.round(overallProgress), costBreakdown, recentActivity);
    }

    private List<ActivityItem> buildRecentActivity(Long businessId) {
        Stream<ActivityItem> expenseActivity = expenseRepository
                .findTop5ByProjectBusinessIdOrderByCreatedAtDesc(businessId).stream()
                .map(this::toActivityItem);

        Stream<ActivityItem> reportActivity = dailyReportRepository
                .findTop5ByProjectBusinessIdOrderByCreatedAtDesc(businessId).stream()
                .map(this::toActivityItem);

        return Stream.concat(expenseActivity, reportActivity)
                .sorted(Comparator.comparing(ActivityItem::occurredAt).reversed())
                .limit(5)
                .toList();
    }

    private ActivityItem toActivityItem(Expense expense) {
        return new ActivityItem(
                "EXPENSE",
                "%s expense of %s added".formatted(expense.getCategory().name(), expense.getAmount()),
                expense.getProject().getName(),
                expense.getCreatedAt()
        );
    }

    private ActivityItem toActivityItem(DailyReport report) {
        return new ActivityItem(
                "DAILY_REPORT",
                "%s submitted a daily report".formatted(report.getSubmittedBy().getFullName()),
                report.getProject().getName(),
                report.getCreatedAt()
        );
    }
}
