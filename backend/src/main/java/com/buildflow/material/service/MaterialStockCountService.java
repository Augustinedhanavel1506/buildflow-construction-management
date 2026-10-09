package com.buildflow.material.service;

import com.buildflow.auth.entity.Role;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.material.dto.MaterialStockCountRequest;
import com.buildflow.material.dto.MaterialStockCountResponse;
import com.buildflow.material.entity.Material;
import com.buildflow.material.entity.MaterialStockCount;
import com.buildflow.material.repository.MaterialRepository;
import com.buildflow.material.repository.MaterialStockCountRepository;
import com.buildflow.notification.entity.NotificationType;
import com.buildflow.notification.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MaterialStockCountService {

    private final MaterialStockCountRepository materialStockCountRepository;
    private final MaterialRepository materialRepository;
    private final NotificationService notificationService;
    private final CurrentUserProvider currentUserProvider;

    public MaterialStockCountService(MaterialStockCountRepository materialStockCountRepository,
                                      MaterialRepository materialRepository,
                                      NotificationService notificationService,
                                      CurrentUserProvider currentUserProvider) {
        this.materialStockCountRepository = materialStockCountRepository;
        this.materialRepository = materialRepository;
        this.notificationService = notificationService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<MaterialStockCountResponse> list(Long materialId) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        materialRepository.findByIdAndProjectBusinessId(materialId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found."));

        return materialStockCountRepository.findByMaterialIdOrderByCountDateDesc(materialId).stream()
                .map(MaterialStockCountResponse::from)
                .toList();
    }

    @Transactional
    public MaterialStockCountResponse create(Long materialId, MaterialStockCountRequest request) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        Material material = materialRepository.findByIdAndProjectBusinessId(materialId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found."));

        MaterialStockCount count = new MaterialStockCount();
        count.setMaterial(material);
        count.setCountDate(request.countDate());
        count.setSystemStock(material.getCurrentStock());
        count.setCountedStock(request.countedStock());
        count.setVariance(request.countedStock().subtract(material.getCurrentStock()));
        count.setNotes(request.notes());
        count.setCountedBy(currentUserProvider.getCurrentUser());
        materialStockCountRepository.save(count);

        // A physical count is ground truth: true up the system stock to match reality.
        material.setCurrentStock(request.countedStock());
        materialRepository.save(material);

        if (count.getVariance().signum() < 0) {
            String message = "Physical count found %s short of %s for %s (expected %s, counted %s)".formatted(
                    count.getVariance().abs(), material.getName(), material.getProject().getName(),
                    count.getSystemStock(), count.getCountedStock());
            notificationService.notifyRole(material.getProject().getBusiness(), Role.ADMIN, NotificationType.STOCK_VARIANCE,
                    "Stock shortage detected", message, "MATERIAL", material.getId(), material.getProject().getId());
            notificationService.notifyRole(material.getProject().getBusiness(), Role.PROJECT_MANAGER, NotificationType.STOCK_VARIANCE,
                    "Stock shortage detected", message, "MATERIAL", material.getId(), material.getProject().getId());
        }

        return MaterialStockCountResponse.from(count);
    }
}
