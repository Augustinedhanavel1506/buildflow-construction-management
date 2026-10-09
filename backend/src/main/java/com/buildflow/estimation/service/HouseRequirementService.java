package com.buildflow.estimation.service;

import com.buildflow.business.entity.Business;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.estimation.dto.FloorRequirementRequest;
import com.buildflow.estimation.dto.HouseRequirementRequest;
import com.buildflow.estimation.dto.HouseRequirementResponse;
import com.buildflow.estimation.entity.FloorRequirement;
import com.buildflow.estimation.entity.HouseRequirement;
import com.buildflow.estimation.entity.HouseRequirementStatus;
import com.buildflow.estimation.entity.RoofType;
import com.buildflow.estimation.entity.StructureType;
import com.buildflow.estimation.entity.WallMaterial;
import com.buildflow.estimation.repository.HouseRequirementRepository;
import com.buildflow.project.entity.Project;
import com.buildflow.project.entity.ProjectStatus;
import com.buildflow.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class HouseRequirementService {

    private final HouseRequirementRepository houseRequirementRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserProvider currentUserProvider;

    public HouseRequirementService(HouseRequirementRepository houseRequirementRepository,
                                    ProjectRepository projectRepository,
                                    CurrentUserProvider currentUserProvider) {
        this.houseRequirementRepository = houseRequirementRepository;
        this.projectRepository = projectRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public HouseRequirementResponse create(HouseRequirementRequest request) {
        Business business = currentUserProvider.getCurrentUser().getBusiness();

        // A house requirement always gets its own PLANNING-status Project so the generated BOQ,
        // and everything downstream (pricing, execution tracking), has somewhere to live even
        // before a real contract is signed.
        Project project = new Project();
        project.setBusiness(business);
        project.setContractValue(BigDecimal.ZERO);
        project.setEstimatedCost(BigDecimal.ZERO);
        project.setStatus(ProjectStatus.PLANNING);

        HouseRequirement requirement = new HouseRequirement();
        requirement.setBusiness(business);
        requirement.setProject(project);
        requirement.setStatus(HouseRequirementStatus.DRAFT);

        applyRequest(requirement, request);
        projectRepository.save(project);

        return HouseRequirementResponse.from(houseRequirementRepository.save(requirement));
    }

    @Transactional
    public HouseRequirementResponse update(Long id, HouseRequirementRequest request) {
        HouseRequirement requirement = findOwned(id);

        applyRequest(requirement, request);

        // The plot/checklist changed, so any BOQ generated for the old numbers is stale. Fall back
        // to DRAFT rather than silently keeping "ESTIMATED" against requirements that no longer
        // match it; the existing BoqItems are left as-is until generate-boq is called again, which
        // replaces the SYSTEM_PRELIMINARY lines the same way a plain regenerate does.
        requirement.setStatus(HouseRequirementStatus.DRAFT);

        return HouseRequirementResponse.from(houseRequirementRepository.save(requirement));
    }

    @Transactional(readOnly = true)
    public List<HouseRequirementResponse> list() {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return houseRequirementRepository.findByBusinessIdOrderByCreatedAtDesc(businessId).stream()
                .map(HouseRequirementResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public HouseRequirementResponse get(Long id) {
        return HouseRequirementResponse.from(findOwned(id));
    }

    private HouseRequirement findOwned(Long id) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return houseRequirementRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("House requirement not found."));
    }

    private void applyRequest(HouseRequirement requirement, HouseRequirementRequest request) {
        requirement.setLabel(request.label());
        requirement.setLocation(request.location());
        requirement.setDistrict(request.district() == null || request.district().isBlank() ? null : request.district().trim());
        requirement.setPlotWidthFt(request.plotWidthFt());
        requirement.setPlotLengthFt(request.plotLengthFt());
        requirement.setConstructionGrade(request.constructionGrade());
        requirement.setStructureType(request.structureType() != null ? request.structureType() : StructureType.RCC_FRAMED);
        requirement.setWallMaterial(request.wallMaterial() != null ? request.wallMaterial() : WallMaterial.RED_BRICK);
        requirement.setRoofType(request.roofType() != null ? request.roofType() : RoofType.RCC_SLAB);

        requirement.getProject().setName(request.label());
        requirement.getProject().setLocation(request.location());

        // Replace the floor list in place (same collection instance) so Hibernate's orphanRemoval
        // deletes floors that were removed, rather than reassigning to a detached new list.
        requirement.getFloors().clear();
        for (FloorRequirementRequest floorRequest : request.floors()) {
            requirement.getFloors().add(toFloorEntity(requirement, floorRequest));
        }
    }

    private FloorRequirement toFloorEntity(HouseRequirement requirement, FloorRequirementRequest request) {
        FloorRequirement floor = new FloorRequirement();
        floor.setHouseRequirement(requirement);
        floor.setFloorLevel(request.floorLevel());
        floor.setFloorAreaSqft(request.floorAreaSqft());
        floor.setBedroomCount(request.bedroomCount() != null ? request.bedroomCount() : 0);
        floor.setBathroomCount(request.bathroomCount() != null ? request.bathroomCount() : 0);
        floor.setHasKitchen(Boolean.TRUE.equals(request.hasKitchen()));
        floor.setHasHall(Boolean.TRUE.equals(request.hasHall()));
        floor.setHasBalcony(Boolean.TRUE.equals(request.hasBalcony()));
        floor.setHasPoojaRoom(Boolean.TRUE.equals(request.hasPoojaRoom()));
        floor.setDoorCount(request.doorCount() != null ? request.doorCount() : 0);
        floor.setWindowCount(request.windowCount() != null ? request.windowCount() : 0);
        return floor;
    }
}
