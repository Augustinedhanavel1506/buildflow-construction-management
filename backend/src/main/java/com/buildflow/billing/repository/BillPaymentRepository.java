package com.buildflow.billing.repository;

import com.buildflow.billing.entity.BillPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface BillPaymentRepository extends JpaRepository<BillPayment, Long> {
    List<BillPayment> findByRaBillIdOrderByPaymentDateDesc(Long raBillId);

    @Query("select coalesce(sum(p.amount), 0) from BillPayment p where p.raBill.id = :raBillId")
    BigDecimal sumAmountByRaBillId(@Param("raBillId") Long raBillId);

    @Query("select coalesce(sum(p.amount), 0) from BillPayment p where p.raBill.project.id = :projectId")
    BigDecimal sumAmountByProjectId(@Param("projectId") Long projectId);

    @Query("select coalesce(sum(p.amount), 0) from BillPayment p where p.raBill.project.business.id = :businessId")
    BigDecimal sumAmountByBusinessId(@Param("businessId") Long businessId);
}
