package com.buildflow.material.service;

import com.buildflow.common.exception.BadRequestException;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.material.dto.MaterialUsageRequest;
import com.buildflow.material.dto.MaterialUsageResponse;
import com.buildflow.material.entity.Material;
import com.buildflow.material.entity.MaterialUsage;
import com.buildflow.material.repository.MaterialRepository;
import com.buildflow.material.repository.MaterialUsageRepository;
import com.buildflow.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MaterialUsageService {

    private final MaterialUsageRepository materialUsageRepository;
    private final MaterialRepository materialRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserProvider currentUserProvider;

    public MaterialUsageService(MaterialUsageRepository materialUsageRepository,
                                 MaterialRepository materialRepository,
                                 ProjectRepository projectRepository,
                                 CurrentUserProvider currentUserProvider) {
        this.materialUsageRepository = materialUsageRepository;
        this.materialRepository = materialRepository;
        this.projectRepository = projectRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<MaterialUsageResponse> list(Long projectId) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        projectRepository.findByIdAndBusinessId(projectId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));

        return materialUsageRepository.findByMaterialProjectIdOrderByUsageDateDesc(projectId).stream()
                .map(MaterialUsageResponse::from)
                .toList();
    }

    @Transactional
    public MaterialUsageResponse create(Long materialId, MaterialUsageRequest request) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        Material material = materialRepository.findByIdAndProjectBusinessId(materialId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found."));

        if (material.getCurrentStock().compareTo(request.quantity()) < 0) {
            throw new BadRequestException("Not enough stock available for this material.");
        }

        MaterialUsage usage = new MaterialUsage();
        usage.setMaterial(material);
        usage.setQuantity(request.quantity());
        usage.setUsageDate(request.usageDate());
        usage.setNotes(request.notes());
        usage.setCreatedBy(currentUserProvider.getCurrentUser());
        materialUsageRepository.save(usage);

        material.setCurrentStock(material.getCurrentStock().subtract(request.quantity()));
        materialRepository.save(material);

        return MaterialUsageResponse.from(usage);
    }
}
