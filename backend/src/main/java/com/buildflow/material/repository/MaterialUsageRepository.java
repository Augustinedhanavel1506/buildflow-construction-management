package com.buildflow.material.repository;

import com.buildflow.material.entity.MaterialUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface MaterialUsageRepository extends JpaRepository<MaterialUsage, Long> {
    List<MaterialUsage> findByMaterialProjectIdOrderByUsageDateDesc(Long projectId);

    @Query("select coalesce(sum(u.quantity), 0) from MaterialUsage u where u.material.id = :materialId")
    BigDecimal sumQuantityByMaterialId(@Param("materialId") Long materialId);
}
