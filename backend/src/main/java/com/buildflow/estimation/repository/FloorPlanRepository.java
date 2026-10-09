package com.buildflow.estimation.repository;

import com.buildflow.estimation.entity.FloorPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FloorPlanRepository extends JpaRepository<FloorPlan, Long> {
    List<FloorPlan> findByHouseRequirementIdOrderByFloorLevelAsc(Long houseRequirementId);

    Optional<FloorPlan> findByHouseRequirementIdAndFloorLevel(Long houseRequirementId, int floorLevel);
}
