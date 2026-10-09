package com.buildflow.material.service;

import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.material.dto.MaterialRequest;
import com.buildflow.material.dto.MaterialResponse;
import com.buildflow.material.entity.Material;
import com.buildflow.material.repository.MaterialRepository;
import com.buildflow.project.entity.Project;
import com.buildflow.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserProvider currentUserProvider;

    public MaterialService(MaterialRepository materialRepository,
                            ProjectRepository projectRepository,
                            CurrentUserProvider currentUserProvider) {
        this.materialRepository = materialRepository;
        this.projectRepository = projectRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public List<MaterialResponse> list(Long projectId) {
        Project project = findOwnedProject(projectId);
        return materialRepository.findByProjectIdOrderByNameAsc(project.getId()).stream()
                .map(MaterialResponse::from)
                .toList();
    }

    @Transactional
    public MaterialResponse create(Long projectId, MaterialRequest request) {
        Project project = findOwnedProject(projectId);

        Material material = new Material();
        material.setProject(project);
        material.setName(request.name());
        material.setUnit(request.unit());
        material.setMinimumStock(request.minimumStock());
        material.setCurrentStock(request.openingStock() != null ? request.openingStock() : BigDecimal.ZERO);

        return MaterialResponse.from(materialRepository.save(material));
    }

    @Transactional
    public MaterialResponse update(Long id, MaterialRequest request) {
        Material material = findOwnedMaterial(id);
        material.setName(request.name());
        material.setUnit(request.unit());
        material.setMinimumStock(request.minimumStock());
        return MaterialResponse.from(materialRepository.save(material));
    }

    private Project findOwnedProject(Long projectId) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return projectRepository.findByIdAndBusinessId(projectId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
    }

    private Material findOwnedMaterial(Long id) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return materialRepository.findByIdAndProjectBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found."));
    }
}
