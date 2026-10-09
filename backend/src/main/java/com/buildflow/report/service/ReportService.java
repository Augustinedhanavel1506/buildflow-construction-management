package com.buildflow.report.service;

import com.buildflow.boq.dto.BoqItemResponse;
import com.buildflow.boq.entity.BoqItem;
import com.buildflow.boq.repository.BoqItemRepository;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.common.util.CsvUtils;
import com.buildflow.expense.entity.Expense;
import com.buildflow.expense.entity.ExpenseCategory;
import com.buildflow.expense.repository.ExpenseRepository;
import com.buildflow.progress.repository.ProjectProgressStageRepository;
import com.buildflow.project.entity.Project;
import com.buildflow.project.repository.ProjectRepository;
import com.buildflow.report.dto.BoqVarianceReportResponse;
import com.buildflow.report.dto.ProfitabilityReportResponse;
import com.buildflow.report.dto.ProfitabilityReportResponse.ProjectProfitability;
import com.buildflow.report.dto.ProjectCostReportResponse;
import com.buildflow.report.dto.ProjectCostReportResponse.BoqVarianceTotals;
import com.buildflow.report.dto.ProjectCostReportResponse.CategoryAmount;
import com.buildflow.variation.service.VariationOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final ProjectRepository projectRepository;
    private final ExpenseRepository expenseRepository;
    private final BoqItemRepository boqItemRepository;
    private final ProjectProgressStageRepository progressStageRepository;
    private final VariationOrderService variationOrderService;
    private final CurrentUserProvider currentUserProvider;

    public ReportService(ProjectRepository projectRepository,
                          ExpenseRepository expenseRepository,
                          BoqItemRepository boqItemRepository,
                          ProjectProgressStageRepository progressStageRepository,
                          VariationOrderService variationOrderService,
                          CurrentUserProvider currentUserProvider) {
        this.projectRepository = projectRepository;
        this.expenseRepository = expenseRepository;
        this.boqItemRepository = boqItemRepository;
        this.progressStageRepository = progressStageRepository;
        this.variationOrderService = variationOrderService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public ProjectCostReportResponse getProjectCostReport(Long projectId) {
        Project project = findOwnedProject(projectId);

        BigDecimal actualCost = expenseRepository.sumAmountByProjectId(project.getId());
        BigDecimal estimatedCost = project.getEstimatedCost();
        BigDecimal remainingEstimatedCost = estimatedCost.subtract(actualCost).max(BigDecimal.ZERO);
        BigDecimal estimatedFinalCost = actualCost.max(estimatedCost);
        BigDecimal revisedContractValue = project.getContractValue()
                .add(variationOrderService.getNetApprovedAmount(project.getId()));
        BigDecimal estimatedMargin = revisedContractValue.subtract(estimatedFinalCost);
        int overallProgress = (int) Math.round(progressStageRepository.averagePercentCompleteByProjectId(project.getId()));

        List<CategoryAmount> expenseBreakdown = expenseRepository.findCategoryBreakdownByProjectId(project.getId()).stream()
                .map(row -> new CategoryAmount(((ExpenseCategory) row[0]).name(), (BigDecimal) row[1]))
                .toList();

        BoqVarianceTotals boqVariance = computeBoqTotals(project.getId());

        return new ProjectCostReportResponse(
                project.getId(),
                project.getName(),
                project.getContractValue(),
                revisedContractValue,
                estimatedCost,
                actualCost,
                remainingEstimatedCost,
                estimatedFinalCost,
                estimatedMargin,
                overallProgress,
                expenseBreakdown,
                boqVariance
        );
    }

    @Transactional(readOnly = true)
    public BoqVarianceReportResponse getBoqVarianceReport(Long projectId) {
        Project project = findOwnedProject(projectId);
        List<BoqItem> items = boqItemRepository.findByProjectIdOrderByCreatedAtAsc(project.getId());

        BigDecimal totalEstimated = items.stream().map(BoqItem::getEstimatedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalActual = items.stream().map(BoqItem::getActualAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BoqVarianceReportResponse(
                items.stream().map(BoqItemResponse::from).toList(),
                totalEstimated,
                totalActual,
                totalActual.subtract(totalEstimated)
        );
    }

    @Transactional(readOnly = true)
    public ProfitabilityReportResponse getProfitabilityReport() {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        List<Project> projects = projectRepository.findByBusinessIdAndArchivedFalse(businessId);

        List<ProjectProfitability> rows = projects.stream()
                .map(project -> {
                    BigDecimal actualCost = expenseRepository.sumAmountByProjectId(project.getId());
                    BigDecimal estimatedFinalCost = actualCost.max(project.getEstimatedCost());
                    BigDecimal revisedContractValue = project.getContractValue()
                            .add(variationOrderService.getNetApprovedAmount(project.getId()));
                    BigDecimal margin = revisedContractValue.subtract(estimatedFinalCost);
                    BigDecimal marginPercent = revisedContractValue.signum() > 0
                            ? margin.multiply(BigDecimal.valueOf(100)).divide(revisedContractValue, 1, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return new ProjectProfitability(
                            project.getId(), project.getName(), project.getStatus().name(),
                            project.getContractValue(), revisedContractValue, estimatedFinalCost, margin, marginPercent);
                })
                .toList();

        BigDecimal totalContractValue = rows.stream().map(ProjectProfitability::revisedContractValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEstimatedFinalCost = rows.stream().map(ProjectProfitability::estimatedFinalCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEstimatedMargin = rows.stream().map(ProjectProfitability::estimatedMargin).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ProfitabilityReportResponse(rows, totalContractValue, totalEstimatedFinalCost, totalEstimatedMargin);
    }

    @Transactional(readOnly = true)
    public String exportBoqVarianceCsv(Long projectId) {
        BoqVarianceReportResponse report = getBoqVarianceReport(projectId);

        List<String> headers = List.of("Item", "Category", "Unit", "Quantity", "Rate", "Estimated Amount", "Actual Amount", "Variance");
        List<List<String>> rows = report.items().stream()
                .map(item -> List.of(
                        item.itemName(), item.category(), item.unit(),
                        item.quantity().toString(), item.rate().toString(),
                        item.estimatedAmount().toString(), item.actualAmount().toString(), item.variance().toString()))
                .collect(Collectors.toCollection(ArrayList::new));
        rows.add(List.of("TOTAL", "", "", "", "",
                report.totalEstimated().toString(), report.totalActual().toString(), report.totalVariance().toString()));

        return CsvUtils.buildCsv(headers, rows);
    }

    @Transactional(readOnly = true)
    public String exportExpensesCsv(Long projectId) {
        Project project = findOwnedProject(projectId);
        List<Expense> expenses = expenseRepository.findByProjectIdOrderByDateDesc(project.getId());

        List<String> headers = List.of("Date", "Category", "Supplier", "Description", "Amount", "Added By");
        List<List<String>> rows = expenses.stream()
                .map(expense -> List.of(
                        expense.getDate().toString(),
                        expense.getCategory().name(),
                        expense.getSupplierName() == null ? "" : expense.getSupplierName(),
                        expense.getDescription() == null ? "" : expense.getDescription(),
                        expense.getAmount().toString(),
                        expense.getCreatedBy().getFullName()))
                .toList();

        return CsvUtils.buildCsv(headers, rows);
    }

    @Transactional(readOnly = true)
    public String exportProfitabilityCsv() {
        ProfitabilityReportResponse report = getProfitabilityReport();

        List<String> headers = List.of(
                "Project", "Status", "Original Contract Value", "Revised Contract Value",
                "Estimated Final Cost", "Estimated Margin", "Margin %");
        List<List<String>> rows = report.projects().stream()
                .map(row -> List.of(
                        row.projectName(), row.status(), row.contractValue().toString(), row.revisedContractValue().toString(),
                        row.estimatedFinalCost().toString(), row.estimatedMargin().toString(),
                        row.marginPercent().toString()))
                .toList();

        return CsvUtils.buildCsv(headers, rows);
    }

    private BoqVarianceTotals computeBoqTotals(Long projectId) {
        List<BoqItem> items = boqItemRepository.findByProjectIdOrderByCreatedAtAsc(projectId);
        BigDecimal totalEstimated = items.stream().map(BoqItem::getEstimatedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalActual = items.stream().map(BoqItem::getActualAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BoqVarianceTotals(totalEstimated, totalActual, totalActual.subtract(totalEstimated));
    }

    private Project findOwnedProject(Long projectId) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return projectRepository.findByIdAndBusinessId(projectId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
    }
}
