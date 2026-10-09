package com.buildflow.material.repository;

import com.buildflow.material.entity.MaterialPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface MaterialPurchaseRepository extends JpaRepository<MaterialPurchase, Long> {
    List<MaterialPurchase> findByMaterialProjectIdOrderByPurchaseDateDesc(Long projectId);

    @Query("select coalesce(sum(p.quantity), 0) from MaterialPurchase p where p.material.id = :materialId")
    BigDecimal sumQuantityByMaterialId(@Param("materialId") Long materialId);

    @Query("select coalesce(sum(p.quantity * p.rate), 0) from MaterialPurchase p where p.material.id = :materialId")
    BigDecimal sumAmountByMaterialId(@Param("materialId") Long materialId);
}
