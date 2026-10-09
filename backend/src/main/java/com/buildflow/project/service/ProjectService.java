package com.buildflow.project.service;

import com.buildflow.business.entity.Business;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.expense.repository.ExpenseRepository;
import com.buildflow.progress.repository.ProjectProgressStageRepository;
import com.buildflow.project.dto.ProjectRequest;
import com.buildflow.project.dto.ProjectResponse;
import com.buildflow.project.entity.Project;
import com.buildflow.project.entity.ProjectStatus;
import com.buildflow.project.repository.ProjectRepository;
import com.buildflow.variation.service.VariationOrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ExpenseRepository expenseRepository;
    private final ProjectProgressStageRepository progressStageRepository;
    private final VariationOrderService variationOrderService;
    private final CurrentUserProvider currentUserProvider;

    public ProjectService(ProjectRepository projectRepository,
                           ExpenseRepository expenseRepository,
                           ProjectProgressStageRepository progressStageRepository,
                           VariationOrderService variationOrderService,
                           CurrentUserProvider currentUserProvider) {
        this.projectRepository = projectRepository;
        this.expenseRepository = expenseRepository;
        this.progressStageRepository = progressStageRepository;
        this.variationOrderService = variationOrderService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        Business business = currentUserProvider.getCurrentUser().getBusiness();

        Project project = new Project();
        project.setBusiness(business);
        applyRequest(project, request);

        Project saved = projectRepository.save(project);
        return ProjectResponse.from(saved, BigDecimal.ZERO, 0, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> list(Pageable pageable) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return projectRepository.findByBusinessIdAndArchivedFalse(businessId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long id) {
        return toResponse(findOwnedProject(id));
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = findOwnedProject(id);
        applyRequest(project, request);
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse updateStatus(Long id, ProjectStatus status) {
        Project project = findOwnedProject(id);
        project.setStatus(status);
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public void archive(Long id) {
        Project project = findOwnedProject(id);
        project.setArchived(true);
        projectRepository.save(project);
    }

    private ProjectResponse toResponse(Project project) {
        return ProjectResponse.from(
                project,
                expenseRepository.sumAmountByProjectId(project.getId()),
                progressStageRepository.averagePercentCompleteByProjectId(project.getId()),
                variationOrderService.getNetApprovedAmount(project.getId()));
    }

    private Project findOwnedProject(Long id) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return projectRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
    }

    private void applyRequest(Project project, ProjectRequest request) {
        project.setName(request.name());
        project.setClientName(request.clientName());
        project.setClientGstin(request.clientGstin());
        project.setClientAddress(request.clientAddress());
        project.setLocation(request.location());
        project.setContractValue(request.contractValue());
        project.setEstimatedCost(request.estimatedCost() != null ? request.estimatedCost() : BigDecimal.ZERO);
        project.setStartDate(request.startDate());
        project.setExpectedEndDate(request.expectedEndDate());
    }
}
