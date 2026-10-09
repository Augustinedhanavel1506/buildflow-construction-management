package com.buildflow.material.service;

import com.buildflow.auth.entity.Role;
import com.buildflow.common.exception.BadRequestException;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.material.dto.MaterialRequisitionRequest;
import com.buildflow.material.dto.MaterialRequisitionResponse;
import com.buildflow.material.entity.Material;
import com.buildflow.material.entity.MaterialRequisition;
import com.buildflow.material.entity.MaterialRequisitionStatus;
import com.buildflow.material.repository.MaterialRepository;
import com.buildflow.material.repository.MaterialRequisitionRepository;
import com.buildflow.notification.entity.NotificationType;
import com.buildflow.notification.service.NotificationService;
import com.buildflow.project.entity.Project;
import com.buildflow.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class MaterialRequisitionService {

    private final MaterialRequisitionRepository materialRequisitionRepository;
    private final MaterialRepository materialRepository;
    private final ProjectRepository projectRepository;
    private final NotificationService notificationService;
    private final CurrentUserProvider currentUserProvider;

    public MaterialRequisitionService(MaterialRequisitionRepository materialRequisitionRepository,
                                       MaterialRepository materialRepository,
                                       ProjectRepository projectRepository,
                                       NotificationService notificationService,
                                       CurrentUserProvider currentUserProvider) {
        this.materialRequisitionRepository = materialRequisitionRepository;
        this.materialRepository = materialRepository;
        this.projectRepository = projectRepository;
        this.notificationService = notificationService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<MaterialRequisitionResponse> list(Long projectId) {
        Project project = findOwnedProject(projectId);
        return materialRequisitionRepository.findByProjectIdOrderByCreatedAtDesc(project.getId()).stream()
                .map(MaterialRequisitionResponse::from)
                .toList();
    }

    @Transactional
    public MaterialRequisitionResponse create(Long projectId, MaterialRequisitionRequest request) {
        Project project = findOwnedProject(projectId);
        Long businessId = currentUserProvider.getCurrentBusinessId();

        Material material = materialRepository.findByIdAndProjectBusinessId(request.materialId(), businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found."));

        MaterialRequisition requisition = new MaterialRequisition();
        requisition.setProject(project);
        requisition.setMaterial(material);
        requisition.setQuantity(request.quantity());
        requisition.setRequiredDate(request.requiredDate());
        requisition.setReason(request.reason());
        requisition.setRequestedBy(currentUserProvider.getCurrentUser());

        MaterialRequisition saved = materialRequisitionRepository.save(requisition);

        notificationService.notifyRole(project.getBusiness(), Role.ADMIN, NotificationType.MATERIAL_REQUEST,
                "Material request pending",
                "%s requested %s %s of %s for %s".formatted(
                        saved.getRequestedBy().getFullName(), saved.getQuantity(), material.getUnit(),
                        material.getName(), project.getName()),
                "MATERIAL_REQUISITION", saved.getId(), project.getId());
        notificationService.notifyRole(project.getBusiness(), Role.PROJECT_MANAGER, NotificationType.MATERIAL_REQUEST,
                "Material request pending",
                "%s requested %s %s of %s for %s".formatted(
                        saved.getRequestedBy().getFullName(), saved.getQuantity(), material.getUnit(),
                        material.getName(), project.getName()),
                "MATERIAL_REQUISITION", saved.getId(), project.getId());

        return MaterialRequisitionResponse.from(saved);
    }

    @Transactional
    public MaterialRequisitionResponse approve(Long id) {
        MaterialRequisition requisition = findOwnedPendingRequisition(id);
        requisition.setStatus(MaterialRequisitionStatus.APPROVED);
        requisition.setDecidedBy(currentUserProvider.getCurrentUser());
        requisition.setDecidedAt(Instant.now());
        MaterialRequisition saved = materialRequisitionRepository.save(requisition);
        notifyDecision(saved, "approved");
        return MaterialRequisitionResponse.from(saved);
    }

    @Transactional
    public MaterialRequisitionResponse reject(Long id) {
        MaterialRequisition requisition = findOwnedPendingRequisition(id);
        requisition.setStatus(MaterialRequisitionStatus.REJECTED);
        requisition.setDecidedBy(currentUserProvider.getCurrentUser());
        requisition.setDecidedAt(Instant.now());
        MaterialRequisition saved = materialRequisitionRepository.save(requisition);
        notifyDecision(saved, "rejected");
        return MaterialRequisitionResponse.from(saved);
    }

    private void notifyDecision(MaterialRequisition requisition, String decision) {
        notificationService.notifyRole(
                requisition.getProject().getBusiness(), requisition.getRequestedBy().getRole(),
                NotificationType.MATERIAL_REQUEST_DECISION,
                "Material request " + decision,
                "Your request for %s %s of %s was %s".formatted(
                        requisition.getQuantity(), requisition.getMaterial().getUnit(),
                        requisition.getMaterial().getName(), decision),
                "MATERIAL_REQUISITION", requisition.getId(), requisition.getProject().getId());
    }

    private MaterialRequisition findOwnedPendingRequisition(Long id) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        MaterialRequisition requisition = materialRequisitionRepository.findByIdAndProjectBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Material request not found."));
        if (requisition.getStatus() != MaterialRequisitionStatus.PENDING) {
            throw new BadRequestException("This request has already been reviewed.");
        }
        return requisition;
    }

    private Project findOwnedProject(Long projectId) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return projectRepository.findByIdAndBusinessId(projectId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
    }
}
