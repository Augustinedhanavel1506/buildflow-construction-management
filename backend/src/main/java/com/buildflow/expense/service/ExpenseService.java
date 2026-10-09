package com.buildflow.expense.service;

import com.buildflow.auth.entity.Role;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.expense.dto.ExpenseRequest;
import com.buildflow.expense.dto.ExpenseResponse;
import com.buildflow.expense.entity.Expense;
import com.buildflow.expense.repository.ExpenseRepository;
import com.buildflow.notification.entity.NotificationType;
import com.buildflow.notification.service.NotificationService;
import com.buildflow.project.entity.Project;
import com.buildflow.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ProjectRepository projectRepository;
    private final NotificationService notificationService;
    private final CurrentUserProvider currentUserProvider;

    public ExpenseService(ExpenseRepository expenseRepository,
                           ProjectRepository projectRepository,
                           NotificationService notificationService,
                           CurrentUserProvider currentUserProvider) {
        this.expenseRepository = expenseRepository;
        this.projectRepository = projectRepository;
        this.notificationService = notificationService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(Long projectId) {
        Project project = findOwnedProject(projectId);
        return expenseRepository.findByProjectIdOrderByDateDesc(project.getId()).stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(Long id) {
        return ExpenseResponse.from(findOwnedExpense(id));
    }

    @Transactional
    public ExpenseResponse create(Long projectId, ExpenseRequest request) {
        Project project = findOwnedProject(projectId);

        Expense expense = new Expense();
        expense.setProject(project);
        expense.setCreatedBy(currentUserProvider.getCurrentUser());
        applyRequest(expense, request);

        Expense saved = expenseRepository.save(expense);
        notifyIfBudgetJustExceeded(project, saved.getAmount());

        return ExpenseResponse.from(saved);
    }

    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Expense expense = findOwnedExpense(id);
        applyRequest(expense, request);
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(Long id) {
        Expense expense = findOwnedExpense(id);
        expenseRepository.delete(expense);
    }

    private void notifyIfBudgetJustExceeded(Project project, BigDecimal latestExpenseAmount) {
        BigDecimal estimatedCost = project.getEstimatedCost();
        if (estimatedCost.signum() <= 0) {
            return;
        }

        BigDecimal totalActualCost = expenseRepository.sumAmountByProjectId(project.getId());
        BigDecimal costBeforeThisExpense = totalActualCost.subtract(latestExpenseAmount);

        boolean justCrossedBudget = costBeforeThisExpense.compareTo(estimatedCost) <= 0
                && totalActualCost.compareTo(estimatedCost) > 0;

        if (justCrossedBudget) {
            String message = "Actual cost (%s) has exceeded the estimated cost (%s) for %s".formatted(
                    totalActualCost, estimatedCost, project.getName());
            notificationService.notifyRole(project.getBusiness(), Role.ADMIN, NotificationType.BUDGET_VARIANCE,
                    "Budget variance detected", message, "PROJECT", project.getId(), project.getId());
            notificationService.notifyRole(project.getBusiness(), Role.PROJECT_MANAGER, NotificationType.BUDGET_VARIANCE,
                    "Budget variance detected", message, "PROJECT", project.getId(), project.getId());
        }
    }

    private void applyRequest(Expense expense, ExpenseRequest request) {
        expense.setCategory(request.category());
        expense.setAmount(request.amount());
        expense.setDate(request.date());
        expense.setSupplierName(request.supplierName());
        expense.setDescription(request.description());
    }

    private Project findOwnedProject(Long projectId) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return projectRepository.findByIdAndBusinessId(projectId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
    }

    private Expense findOwnedExpense(Long id) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return expenseRepository.findByIdAndProjectBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found."));
    }
}
