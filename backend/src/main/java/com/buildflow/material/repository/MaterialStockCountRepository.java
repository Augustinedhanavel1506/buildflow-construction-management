package com.buildflow.material.repository;

import com.buildflow.material.entity.MaterialStockCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MaterialStockCountRepository extends JpaRepository<MaterialStockCount, Long> {
    List<MaterialStockCount> findByMaterialIdOrderByCountDateDesc(Long materialId);

    Optional<MaterialStockCount> findTopByMaterialIdOrderByCountDateDescCreatedAtDesc(Long materialId);

    @Query("select c from MaterialStockCount c where c.material.project.id = :projectId order by c.countDate desc")
    List<MaterialStockCount> findByProjectIdOrderByCountDateDesc(@Param("projectId") Long projectId);

    @Query("select coalesce(sum(c.variance), 0) from MaterialStockCount c where c.material.id = :materialId")
    BigDecimal sumVarianceByMaterialId(@Param("materialId") Long materialId);
}
