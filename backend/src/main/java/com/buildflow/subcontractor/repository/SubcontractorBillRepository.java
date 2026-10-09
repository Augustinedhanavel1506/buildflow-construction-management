package com.buildflow.subcontractor.repository;

import com.buildflow.subcontractor.entity.SubcontractorBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SubcontractorBillRepository extends JpaRepository<SubcontractorBill, Long> {
    List<SubcontractorBill> findByWorkOrderIdOrderByBillDateDesc(Long workOrderId);

    Optional<SubcontractorBill> findByIdAndWorkOrderProjectBusinessId(Long id, Long businessId);

    @Query("""
            select coalesce(sum(b.tdsAmount), 0) from SubcontractorBill b
            where b.workOrder.project.business.id = :businessId
            """)
    BigDecimal sumTdsByBusinessId(@Param("businessId") Long businessId);
}
