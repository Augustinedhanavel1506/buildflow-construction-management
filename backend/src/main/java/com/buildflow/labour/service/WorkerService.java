package com.buildflow.labour.service;

import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.labour.dto.WorkerRequest;
import com.buildflow.labour.dto.WorkerResponse;
import com.buildflow.labour.entity.Worker;
import com.buildflow.labour.repository.WorkerRepository;
import com.buildflow.project.entity.Project;
import com.buildflow.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserProvider currentUserProvider;

    public WorkerService(WorkerRepository workerRepository,
                          ProjectRepository projectRepository,
                          CurrentUserProvider currentUserProvider) {
        this.workerRepository = workerRepository;
        this.projectRepository = projectRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public List<WorkerResponse> list(Long projectId) {
        Project project = findOwnedProject(projectId);
        return workerRepository.findByProjectIdOrderByNameAsc(project.getId()).stream()
                .map(WorkerResponse::from)
                .toList();
    }

    @Transactional
    public WorkerResponse create(Long projectId, WorkerRequest request) {
        Project project = findOwnedProject(projectId);

        Worker worker = new Worker();
        worker.setProject(project);
        applyRequest(worker, request);

        return WorkerResponse.from(workerRepository.save(worker));
    }

    @Transactional
    public WorkerResponse update(Long id, WorkerRequest request) {
        Worker worker = findOwnedWorker(id);
        applyRequest(worker, request);
        return WorkerResponse.from(workerRepository.save(worker));
    }

    private void applyRequest(Worker worker, WorkerRequest request) {
        worker.setName(request.name());
        worker.setRole(request.role());
        worker.setDailyRate(request.dailyRate());
        worker.setActive(request.active() == null || request.active());
    }

    private Project findOwnedProject(Long projectId) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return projectRepository.findByIdAndBusinessId(projectId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
    }

    private Worker findOwnedWorker(Long id) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return workerRepository.findByIdAndProjectBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found."));
    }
}
