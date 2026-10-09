package com.buildflow.subcontractor.repository;

import com.buildflow.subcontractor.entity.SubcontractorPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface SubcontractorPaymentRepository extends JpaRepository<SubcontractorPayment, Long> {
    List<SubcontractorPayment> findBySubcontractorBillIdOrderByPaymentDateDesc(Long subcontractorBillId);

    @Query("select coalesce(sum(p.amount), 0) from SubcontractorPayment p where p.subcontractorBill.id = :billId")
    BigDecimal sumAmountBySubcontractorBillId(@Param("billId") Long billId);
}
