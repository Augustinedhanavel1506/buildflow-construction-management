package com.buildflow.variation.service;

import com.buildflow.auth.entity.Role;
import com.buildflow.common.exception.BadRequestException;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.notification.entity.NotificationType;
import com.buildflow.notification.service.NotificationService;
import com.buildflow.project.entity.Project;
import com.buildflow.project.repository.ProjectRepository;
import com.buildflow.variation.dto.VariationOrderRequest;
import com.buildflow.variation.dto.VariationOrderResponse;
import com.buildflow.variation.entity.VariationOrder;
import com.buildflow.variation.entity.VariationStatus;
import com.buildflow.variation.entity.VariationType;
import com.buildflow.variation.repository.VariationOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class VariationOrderService {

    private final VariationOrderRepository variationOrderRepository;
    private final ProjectRepository projectRepository;
    private final NotificationService notificationService;
    private final CurrentUserProvider currentUserProvider;

    public VariationOrderService(VariationOrderRepository variationOrderRepository,
                                  ProjectRepository projectRepository,
                                  NotificationService notificationService,
                                  CurrentUserProvider currentUserProvider) {
        this.variationOrderRepository = variationOrderRepository;
        this.projectRepository = projectRepository;
        this.notificationService = notificationService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<VariationOrderResponse> list(Long projectId) {
        Project project = findOwnedProject(projectId);
        return variationOrderRepository.findByProjectIdOrderByCreatedAtDesc(project.getId()).stream()
                .map(VariationOrderResponse::from)
                .toList();
    }

    @Transactional
    public VariationOrderResponse create(Long projectId, VariationOrderRequest request) {
        Project project = findOwnedProject(projectId);

        VariationOrder variation = new VariationOrder();
        variation.setProject(project);
        variation.setTitle(request.title());
        variation.setDescription(request.description());
        variation.setType(request.type());
        variation.setAmount(request.amount());
        variation.setRequestedBy(currentUserProvider.getCurrentUser());

        VariationOrder saved = variationOrderRepository.save(variation);

        String message = "%s (%s) submitted for %s — %s".formatted(
                saved.getTitle(), saved.getType() == VariationType.ADDITION ? "addition" : "omission", project.getName(),
                saved.getAmount());
        notificationService.notifyRole(project.getBusiness(), Role.ADMIN, NotificationType.VARIATION_ORDER,
                "Variation order submitted", message, "VARIATION_ORDER", saved.getId(), project.getId());
        notificationService.notifyRole(project.getBusiness(), Role.PROJECT_MANAGER, NotificationType.VARIATION_ORDER,
                "Variation order submitted", message, "VARIATION_ORDER", saved.getId(), project.getId());

        return VariationOrderResponse.from(saved);
    }

    @Transactional
    public VariationOrderResponse approve(Long id) {
        return decide(id, VariationStatus.APPROVED);
    }

    @Transactional
    public VariationOrderResponse reject(Long id) {
        return decide(id, VariationStatus.REJECTED);
    }

    private VariationOrderResponse decide(Long id, VariationStatus decision) {
        VariationOrder variation = findOwnedVariation(id);
        if (variation.getStatus() != VariationStatus.PENDING) {
            throw new BadRequestException("This variation order has already been reviewed.");
        }
        variation.setStatus(decision);
        variation.setDecidedBy(currentUserProvider.getCurrentUser());
        variation.setDecidedAt(Instant.now());
        return VariationOrderResponse.from(variationOrderRepository.save(variation));
    }

    /**
     * Net effect of all approved variations on the contract value (additions minus omissions).
     */
    @Transactional(readOnly = true)
    public BigDecimal getNetApprovedAmount(Long projectId) {
        BigDecimal additions = variationOrderRepository.sumAmountByProjectIdAndStatusAndType(
                projectId, VariationStatus.APPROVED, VariationType.ADDITION);
        BigDecimal omissions = variationOrderRepository.sumAmountByProjectIdAndStatusAndType(
                projectId, VariationStatus.APPROVED, VariationType.OMISSION);
        return additions.subtract(omissions);
    }

    @Transactional(readOnly = true)
    public BigDecimal getNetApprovedAmountForBusiness(Long businessId) {
        BigDecimal additions = variationOrderRepository.sumAmountByProjectBusinessIdAndStatusAndType(
                businessId, VariationStatus.APPROVED, VariationType.ADDITION);
        BigDecimal omissions = variationOrderRepository.sumAmountByProjectBusinessIdAndStatusAndType(
                businessId, VariationStatus.APPROVED, VariationType.OMISSION);
        return additions.subtract(omissions);
    }

    private Project findOwnedProject(Long projectId) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return projectRepository.findByIdAndBusinessId(projectId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
    }

    private VariationOrder findOwnedVariation(Long id) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return variationOrderRepository.findByIdAndProjectBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Variation order not found."));
    }
}
