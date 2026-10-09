package com.buildflow.estimation.service;

import com.buildflow.business.entity.Business;
import com.buildflow.common.exception.BadRequestException;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.estimation.dto.BoqGenerationRuleRequest;
import com.buildflow.estimation.dto.BoqGenerationRuleResponse;
import com.buildflow.estimation.entity.BoqGenerationRule;
import com.buildflow.estimation.entity.RuleSourceType;
import com.buildflow.estimation.repository.BoqGenerationRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Platform rules are read-only for tenants. Editing one creates (or updates) that business's own
 * override row, which BoqGenerationService prefers over the platform rule for the same base code.
 */
@Service
public class BoqGenerationRuleService {

    private final BoqGenerationRuleRepository ruleRepository;
    private final CurrentUserProvider currentUserProvider;

    public BoqGenerationRuleService(BoqGenerationRuleRepository ruleRepository,
                                     CurrentUserProvider currentUserProvider) {
        this.ruleRepository = ruleRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<BoqGenerationRuleResponse> list() {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        List<BoqGenerationRule> visible = ruleRepository.findVisibleToBusiness(businessId);

        // Hide a platform rule once this business has overridden it, so the list shows what applies.
        java.util.Set<String> overridden = new java.util.HashSet<>();
        for (BoqGenerationRule rule : visible) {
            if (rule.getBusiness() != null) {
                overridden.add(baseCode(rule));
            }
        }
        return visible.stream()
                .filter(rule -> rule.getBusiness() != null || !overridden.contains(baseCode(rule)))
                .map(BoqGenerationRuleResponse::from)
                .toList();
    }

    @Transactional
    public BoqGenerationRuleResponse update(Long id, BoqGenerationRuleRequest request) {
        validate(request);
        Business business = currentUserProvider.getCurrentUser().getBusiness();
        BoqGenerationRule rule = findVisible(id, business.getId());

        if (rule.getBusiness() != null) {
            applyRequest(rule, request);
            rule.setVersion(rule.getVersion() + 1);
            return BoqGenerationRuleResponse.from(ruleRepository.save(rule));
        }

        String overrideCode = baseCode(rule) + "__B" + business.getId();
        BoqGenerationRule override = ruleRepository.findByRuleCode(overrideCode).orElse(null);
        if (override != null) {
            applyRequest(override, request);
            override.setVersion(override.getVersion() + 1);
            return BoqGenerationRuleResponse.from(ruleRepository.save(override));
        }

        BoqGenerationRule created = new BoqGenerationRule();
        created.setRuleCode(overrideCode);
        created.setBaseRuleCode(baseCode(rule));
        created.setComponent(rule.getComponent());
        created.setConstructionGrade(rule.getConstructionGrade());
        created.setItemName(rule.getItemName());
        created.setBoqCategory(rule.getBoqCategory());
        created.setUnit(rule.getUnit());
        created.setBasis(rule.getBasis());
        created.setStructureType(rule.getStructureType());
        created.setWallMaterial(rule.getWallMaterial());
        created.setRoofType(rule.getRoofType());
        created.setApplicableFloorMin(rule.getApplicableFloorMin());
        created.setApplicableFloorMax(rule.getApplicableFloorMax());
        created.setRegion(rule.getRegion());
        created.setEffectiveFrom(LocalDate.now());
        created.setBusiness(business);
        created.setVersion(1);
        applyRequest(created, request);
        return BoqGenerationRuleResponse.from(ruleRepository.save(created));
    }

    // Removes this business's override so the platform rule applies again.
    @Transactional
    public void revertToPlatform(Long id) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        BoqGenerationRule rule = findVisible(id, businessId);
        if (rule.getBusiness() == null) {
            throw new BadRequestException("Only your own overrides can be reverted; platform rules are shared.");
        }
        ruleRepository.delete(rule);
    }

    private void validate(BoqGenerationRuleRequest request) {
        if (request.minCoefficient() != null && request.maxCoefficient() != null
                && request.minCoefficient().compareTo(request.maxCoefficient()) > 0) {
            throw new BadRequestException("Minimum coefficient cannot be greater than maximum coefficient.");
        }
        if (request.minCoefficient() != null && request.minCoefficient().compareTo(request.coefficient()) > 0) {
            throw new BadRequestException("Minimum coefficient cannot be greater than the coefficient.");
        }
        if (request.maxCoefficient() != null && request.maxCoefficient().compareTo(request.coefficient()) < 0) {
            throw new BadRequestException("Maximum coefficient cannot be less than the coefficient.");
        }
        if (Boolean.TRUE.equals(request.verified()) && request.sourceType() == RuleSourceType.PLACEHOLDER) {
            throw new BadRequestException("A placeholder rule cannot be marked verified; choose its real source first.");
        }
    }

    private void applyRequest(BoqGenerationRule rule, BoqGenerationRuleRequest request) {
        rule.setCoefficient(request.coefficient());
        rule.setWastagePercent(request.wastagePercent());
        rule.setMinCoefficient(request.minCoefficient());
        rule.setMaxCoefficient(request.maxCoefficient());
        rule.setSourceType(request.sourceType());
        rule.setSourceReference(request.sourceReference().trim());
        rule.setConfidenceLevel(request.confidenceLevel());
        rule.setVerified(Boolean.TRUE.equals(request.verified()));
        rule.setSampleSize(request.sampleSize());
        rule.setNotes(request.notes());
        rule.setActive(request.active() == null || request.active());
    }

    private BoqGenerationRule findVisible(Long id, Long businessId) {
        return ruleRepository.findById(id)
                .filter(rule -> rule.getBusiness() == null || rule.getBusiness().getId().equals(businessId))
                .orElseThrow(() -> new ResourceNotFoundException("Generation rule not found."));
    }

    private static String baseCode(BoqGenerationRule rule) {
        return rule.getBaseRuleCode() != null ? rule.getBaseRuleCode() : rule.getRuleCode();
    }
}
