package com.buildflow.variation.repository;

import com.buildflow.variation.entity.VariationOrder;
import com.buildflow.variation.entity.VariationStatus;
import com.buildflow.variation.entity.VariationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface VariationOrderRepository extends JpaRepository<VariationOrder, Long> {
    List<VariationOrder> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    Optional<VariationOrder> findByIdAndProjectBusinessId(Long id, Long businessId);

    @Query("""
            select coalesce(sum(v.amount), 0) from VariationOrder v
            where v.project.id = :projectId and v.status = :status and v.type = :type
            """)
    BigDecimal sumAmountByProjectIdAndStatusAndType(
            @Param("projectId") Long projectId, @Param("status") VariationStatus status, @Param("type") VariationType type);

    @Query("""
            select coalesce(sum(v.amount), 0) from VariationOrder v
            where v.project.business.id = :businessId and v.status = :status and v.type = :type
            """)
    BigDecimal sumAmountByProjectBusinessIdAndStatusAndType(
            @Param("businessId") Long businessId, @Param("status") VariationStatus status, @Param("type") VariationType type);
}
