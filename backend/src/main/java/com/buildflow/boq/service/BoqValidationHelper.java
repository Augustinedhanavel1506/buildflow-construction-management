package com.buildflow.boq.service;

import com.buildflow.boq.entity.BoqItem;
import com.buildflow.boq.entity.EstimateSource;
import com.buildflow.boq.repository.BoqItemRepository;
import com.buildflow.project.entity.Project;
import com.buildflow.project.repository.ProjectRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

// Shared by manual validation and the engineer review workflow so both apply identical rules.
@Component
public class BoqValidationHelper {

    private final BoqItemRepository boqItemRepository;
    private final ProjectRepository projectRepository;

    public BoqValidationHelper(BoqItemRepository boqItemRepository, ProjectRepository projectRepository) {
        this.boqItemRepository = boqItemRepository;
        this.projectRepository = projectRepository;
    }

    // A validated line is no longer a range estimate, so its low/high band is cleared.
    public void markValidated(BoqItem item, String validatedBy, BigDecimal correctedQuantity, String note) {
        if (correctedQuantity != null) {
            item.setQuantity(correctedQuantity);
            item.setEstimatedAmount(correctedQuantity.multiply(item.getRate()));
        }
        item.setQuantityLow(null);
        item.setQuantityHigh(null);
        item.setEstimateSource(EstimateSource.ENGINEER_VALIDATED);
        item.setValidatedBy(validatedBy);
        item.setValidatedAt(Instant.now());
        item.setValidationNote(note);
    }

    // A corrected quantity changes a line amount, so the project headline estimate must follow.
    public void refreshProjectEstimate(Project project) {
        BigDecimal total = boqItemRepository.findByProjectIdOrderByCreatedAtAsc(project.getId()).stream()
                .map(BoqItem::getEstimatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        project.setEstimatedCost(total);
        projectRepository.save(project);
    }
}
