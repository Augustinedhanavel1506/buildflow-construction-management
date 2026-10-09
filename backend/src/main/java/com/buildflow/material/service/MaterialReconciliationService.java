package com.buildflow.material.service;

import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.material.dto.MaterialReconciliationResponse;
import com.buildflow.material.dto.MaterialReconciliationResponse.MaterialReconciliationRow;
import com.buildflow.material.entity.Material;
import com.buildflow.material.entity.MaterialStockCount;
import com.buildflow.material.repository.MaterialPurchaseRepository;
import com.buildflow.material.repository.MaterialRepository;
import com.buildflow.material.repository.MaterialStockCountRepository;
import com.buildflow.material.repository.MaterialUsageRepository;
import com.buildflow.project.entity.Project;
import com.buildflow.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class MaterialReconciliationService {

    private final MaterialRepository materialRepository;
    private final MaterialPurchaseRepository materialPurchaseRepository;
    private final MaterialUsageRepository materialUsageRepository;
    private final MaterialStockCountRepository materialStockCountRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserProvider currentUserProvider;

    public MaterialReconciliationService(MaterialRepository materialRepository,
                                          MaterialPurchaseRepository materialPurchaseRepository,
                                          MaterialUsageRepository materialUsageRepository,
                                          MaterialStockCountRepository materialStockCountRepository,
                                          ProjectRepository projectRepository,
                                          CurrentUserProvider currentUserProvider) {
        this.materialRepository = materialRepository;
        this.materialPurchaseRepository = materialPurchaseRepository;
        this.materialUsageRepository = materialUsageRepository;
        this.materialStockCountRepository = materialStockCountRepository;
        this.projectRepository = projectRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalEstimatedVarianceValueForBusiness(Long businessId) {
        return materialRepository.findByProjectBusinessId(businessId).stream()
                .map(material -> toRow(material).estimatedVarianceValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public MaterialReconciliationResponse getReconciliation(Long projectId) {
        Project project = findOwnedProject(projectId);
        List<Material> materials = materialRepository.findByProjectIdOrderByNameAsc(project.getId());

        List<MaterialReconciliationRow> rows = materials.stream().map(this::toRow).toList();
        BigDecimal totalVarianceValue = rows.stream()
                .map(MaterialReconciliationRow::estimatedVarianceValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new MaterialReconciliationResponse(rows, totalVarianceValue);
    }

    private MaterialReconciliationRow toRow(Material material) {
        BigDecimal totalPurchased = materialPurchaseRepository.sumQuantityByMaterialId(material.getId());
        BigDecimal totalUsed = materialUsageRepository.sumQuantityByMaterialId(material.getId());
        BigDecimal purchaseAmount = materialPurchaseRepository.sumAmountByMaterialId(material.getId());
        BigDecimal averageRate = totalPurchased.signum() > 0
                ? purchaseAmount.divide(totalPurchased, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Optional<MaterialStockCount> lastCount = materialStockCountRepository
                .findTopByMaterialIdOrderByCountDateDescCreatedAtDesc(material.getId());
        BigDecimal cumulativeVarianceQty = materialStockCountRepository.sumVarianceByMaterialId(material.getId());
        BigDecimal estimatedVarianceValue = cumulativeVarianceQty.multiply(averageRate);

        return new MaterialReconciliationRow(
                material.getId(),
                material.getName(),
                material.getUnit(),
                material.getCurrentStock(),
                totalPurchased,
                totalUsed,
                averageRate,
                lastCount.map(MaterialStockCount::getCountDate).orElse(null),
                lastCount.map(MaterialStockCount::getVariance).orElse(null),
                cumulativeVarianceQty,
                estimatedVarianceValue
        );
    }

    private Project findOwnedProject(Long projectId) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return projectRepository.findByIdAndBusinessId(projectId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
    }
}
