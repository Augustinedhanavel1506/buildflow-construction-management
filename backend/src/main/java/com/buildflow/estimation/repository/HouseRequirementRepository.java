package com.buildflow.estimation.repository;

import com.buildflow.estimation.entity.HouseRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HouseRequirementRepository extends JpaRepository<HouseRequirement, Long> {
    List<HouseRequirement> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

    Optional<HouseRequirement> findByIdAndBusinessId(Long id, Long businessId);

    Optional<HouseRequirement> findByProjectId(Long projectId);
}
