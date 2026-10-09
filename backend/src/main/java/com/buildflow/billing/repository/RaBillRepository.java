package com.buildflow.billing.repository;

import com.buildflow.billing.entity.RaBill;
import com.buildflow.billing.entity.RaBillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RaBillRepository extends JpaRepository<RaBill, Long> {
    List<RaBill> findByProjectIdOrderByBillDateDesc(Long projectId);

    Optional<RaBill> findByIdAndProjectBusinessId(Long id, Long businessId);

    long countByProjectId(Long projectId);

    @Query("select coalesce(sum(b.netPayableAmount), 0) from RaBill b where b.project.id = :projectId and b.status <> :excludedStatus")
    BigDecimal sumNetPayableByProjectId(@Param("projectId") Long projectId, @Param("excludedStatus") RaBillStatus excludedStatus);

    @Query("select coalesce(sum(b.netPayableAmount), 0) from RaBill b where b.project.business.id = :businessId and b.status <> :excludedStatus")
    BigDecimal sumNetPayableByBusinessId(@Param("businessId") Long businessId, @Param("excludedStatus") RaBillStatus excludedStatus);

    @Query("select coalesce(sum(b.retentionAmount), 0) from RaBill b where b.project.id = :projectId and b.status <> :excludedStatus")
    BigDecimal sumRetentionByProjectId(@Param("projectId") Long projectId, @Param("excludedStatus") RaBillStatus excludedStatus);

    @Query("select coalesce(sum(b.retentionAmount), 0) from RaBill b where b.project.business.id = :businessId and b.status <> :excludedStatus")
    BigDecimal sumRetentionByBusinessId(@Param("businessId") Long businessId, @Param("excludedStatus") RaBillStatus excludedStatus);
}
