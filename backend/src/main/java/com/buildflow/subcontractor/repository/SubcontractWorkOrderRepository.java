package com.buildflow.subcontractor.repository;

import com.buildflow.subcontractor.entity.SubcontractWorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubcontractWorkOrderRepository extends JpaRepository<SubcontractWorkOrder, Long> {
    List<SubcontractWorkOrder> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    Optional<SubcontractWorkOrder> findByIdAndProjectBusinessId(Long id, Long businessId);
}
