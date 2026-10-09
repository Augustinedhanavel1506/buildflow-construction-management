package com.buildflow.boq.service;

import com.buildflow.boq.dto.BoqItemRequest;
import com.buildflow.boq.dto.BoqValidationRequest;
import com.buildflow.boq.entity.EstimateSource;
import com.buildflow.common.exception.BadRequestException;
import com.buildflow.boq.dto.BoqItemResponse;
import com.buildflow.boq.entity.BoqItem;
import com.buildflow.boq.repository.BoqItemRepository;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.project.entity.Project;
import com.buildflow.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BoqItemService {

    private final BoqItemRepository boqItemRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserProvider currentUserProvider;
    private final BoqValidationHelper validationHelper;

    public BoqItemService(BoqItemRepository boqItemRepository,
                           ProjectRepository projectRepository,
                           CurrentUserProvider currentUserProvider,
                           BoqValidationHelper validationHelper) {
        this.boqItemRepository = boqItemRepository;
        this.projectRepository = projectRepository;
        this.currentUserProvider = currentUserProvider;
        this.validationHelper = validationHelper;
    }

    public List<BoqItemResponse> list(Long projectId) {
        Project project = findOwnedProject(projectId);
        return boqItemRepository.findByProjectIdOrderByCreatedAtAsc(project.getId()).stream()
                .map(BoqItemResponse::from)
                .toList();
    }

    @Transactional
    public BoqItemResponse create(Long projectId, BoqItemRequest request) {
        Project project = findOwnedProject(projectId);

        BoqItem item = new BoqItem();
        item.setProject(project);
        applyRequest(item, request);

        return BoqItemResponse.from(boqItemRepository.save(item));
    }

    @Transactional
    public BoqItemResponse update(Long id, BoqItemRequest request) {
        BoqItem item = findOwnedItem(id);
        // Changing the quantity or rate of a generated or validated line makes it the user's own
        // figure, so it must neither keep an engineer's name on it nor be replaced on regenerate.
        boolean figuresChanged = item.getQuantity().compareTo(request.quantity()) != 0
                || item.getRate().compareTo(request.rate()) != 0;
        applyRequest(item, request);
        if (figuresChanged && item.getEstimateSource() != EstimateSource.MANUAL) {
            item.setEstimateSource(EstimateSource.MANUAL);
            item.setQuantityLow(null);
            item.setQuantityHigh(null);
            item.setValidatedBy(null);
            item.setValidatedAt(null);
            item.setValidationNote(null);
        }
        return BoqItemResponse.from(boqItemRepository.save(item));
    }

    // Records an engineer's sign-off on one line, optionally with a corrected quantity. A validated
    // line is no longer a range estimate, so the low/high band is cleared.
    @Transactional
    public BoqItemResponse validate(Long id, BoqValidationRequest request) {
        BoqItem item = findOwnedItem(id);
        if (item.getEstimateSource() == EstimateSource.MANUAL) {
            throw new BadRequestException("Only system-generated lines can be engineer-validated; manual lines are your own figures.");
        }
        validationHelper.markValidated(item, request.engineerName().trim(), request.quantity(), request.note());
        BoqItem saved = boqItemRepository.save(item);
        validationHelper.refreshProjectEstimate(saved.getProject());
        return BoqItemResponse.from(saved);
    }

    // Approves every still-preliminary line in a project exactly as estimated.
    @Transactional
    public List<BoqItemResponse> validateAll(Long projectId, BoqValidationRequest request) {
        Project project = findOwnedProject(projectId);
        List<BoqItem> pending = boqItemRepository.findByProjectIdOrderByCreatedAtAsc(project.getId()).stream()
                .filter(i -> i.getEstimateSource() == EstimateSource.SYSTEM_PRELIMINARY)
                .toList();
        if (pending.isEmpty()) {
            throw new BadRequestException("There are no preliminary lines to validate.");
        }
        for (BoqItem item : pending) {
            validationHelper.markValidated(item, request.engineerName().trim(), null, request.note());
        }
        List<BoqItemResponse> result = boqItemRepository.saveAll(pending).stream().map(BoqItemResponse::from).toList();
        validationHelper.refreshProjectEstimate(project);
        return result;
    }

    @Transactional
    public void delete(Long id) {
        BoqItem item = findOwnedItem(id);
        boqItemRepository.delete(item);
    }

    private void applyRequest(BoqItem item, BoqItemRequest request) {
        item.setItemName(request.itemName());
        item.setCategory(request.category());
        item.setUnit(request.unit());
        item.setQuantity(request.quantity());
        item.setRate(request.rate());
        item.setEstimatedAmount(request.quantity().multiply(request.rate()));
        item.setActualAmount(request.actualAmount() != null ? request.actualAmount() : BigDecimal.ZERO);
    }

    private Project findOwnedProject(Long projectId) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return projectRepository.findByIdAndBusinessId(projectId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
    }

    private BoqItem findOwnedItem(Long id) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return boqItemRepository.findByIdAndProjectBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("BOQ item not found."));
    }
}
