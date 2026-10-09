package com.buildflow.material.service;

import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.material.dto.MaterialPurchaseRequest;
import com.buildflow.material.dto.MaterialPurchaseResponse;
import com.buildflow.material.entity.Material;
import com.buildflow.material.entity.MaterialPurchase;
import com.buildflow.material.repository.MaterialPurchaseRepository;
import com.buildflow.material.repository.MaterialRepository;
import com.buildflow.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MaterialPurchaseService {

    private final MaterialPurchaseRepository materialPurchaseRepository;
    private final MaterialRepository materialRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserProvider currentUserProvider;

    public MaterialPurchaseService(MaterialPurchaseRepository materialPurchaseRepository,
                                    MaterialRepository materialRepository,
                                    ProjectRepository projectRepository,
                                    CurrentUserProvider currentUserProvider) {
        this.materialPurchaseRepository = materialPurchaseRepository;
        this.materialRepository = materialRepository;
        this.projectRepository = projectRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<MaterialPurchaseResponse> list(Long projectId) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        projectRepository.findByIdAndBusinessId(projectId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));

        return materialPurchaseRepository.findByMaterialProjectIdOrderByPurchaseDateDesc(projectId).stream()
                .map(MaterialPurchaseResponse::from)
                .toList();
    }

    @Transactional
    public MaterialPurchaseResponse create(Long materialId, MaterialPurchaseRequest request) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        Material material = materialRepository.findByIdAndProjectBusinessId(materialId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found."));

        MaterialPurchase purchase = new MaterialPurchase();
        purchase.setMaterial(material);
        purchase.setQuantity(request.quantity());
        purchase.setRate(request.rate());
        purchase.setSupplierName(request.supplierName());
        purchase.setPurchaseDate(request.purchaseDate());
        purchase.setCreatedBy(currentUserProvider.getCurrentUser());
        materialPurchaseRepository.save(purchase);

        material.setCurrentStock(material.getCurrentStock().add(request.quantity()));
        materialRepository.save(material);

        return MaterialPurchaseResponse.from(purchase);
    }
}
