package com.buildflow.boq.repository;

import com.buildflow.boq.entity.BoqItem;
import com.buildflow.boq.entity.EstimateSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoqItemRepository extends JpaRepository<BoqItem, Long> {
    List<BoqItem> findByProjectIdOrderByCreatedAtAsc(Long projectId);

    Optional<BoqItem> findByIdAndProjectBusinessId(Long id, Long businessId);

    void deleteByProjectIdAndEstimateSource(Long projectId, EstimateSource estimateSource);
}
