package com.buildflow.material.repository;

import com.buildflow.material.entity.MaterialRequisition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialRequisitionRepository extends JpaRepository<MaterialRequisition, Long> {
    List<MaterialRequisition> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    Optional<MaterialRequisition> findByIdAndProjectBusinessId(Long id, Long businessId);
}
