package com.buildflow.progress.service;

import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.progress.dto.ProgressStageRequest;
import com.buildflow.progress.dto.ProgressStageResponse;
import com.buildflow.progress.dto.ProgressUpdateRequest;
import com.buildflow.progress.entity.ProjectProgressStage;
import com.buildflow.progress.repository.ProjectProgressStageRepository;
import com.buildflow.project.entity.Project;
import com.buildflow.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectProgressService {

    private final ProjectProgressStageRepository progressStageRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserProvider currentUserProvider;

    public ProjectProgressService(ProjectProgressStageRepository progressStageRepository,
                                   ProjectRepository projectRepository,
                                   CurrentUserProvider currentUserProvider) {
        this.progressStageRepository = progressStageRepository;
        this.projectRepository = projectRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public List<ProgressStageResponse> list(Long projectId) {
        Project project = findOwnedProject(projectId);
        return progressStageRepository.findByProjectIdOrderBySortOrderAsc(project.getId()).stream()
                .map(ProgressStageResponse::from)
                .toList();
    }

    @Transactional
    public ProgressStageResponse create(Long projectId, ProgressStageRequest request) {
        Project project = findOwnedProject(projectId);

        ProjectProgressStage stage = new ProjectProgressStage();
        stage.setProject(project);
        stage.setStageName(request.stageName());
        stage.setPercentComplete(request.percentComplete());
        stage.setSortOrder(progressStageRepository.maxSortOrderByProjectId(project.getId()) + 1);

        return ProgressStageResponse.from(progressStageRepository.save(stage));
    }

    @Transactional
    public ProgressStageResponse updatePercent(Long id, ProgressUpdateRequest request) {
        ProjectProgressStage stage = findOwnedStage(id);
        stage.setPercentComplete(request.percentComplete());
        return ProgressStageResponse.from(progressStageRepository.save(stage));
    }

    private ProjectProgressStage findOwnedStage(Long id) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return progressStageRepository.findByIdAndProjectBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Progress stage not found."));
    }

    private Project findOwnedProject(Long projectId) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return projectRepository.findByIdAndBusinessId(projectId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
    }
}
