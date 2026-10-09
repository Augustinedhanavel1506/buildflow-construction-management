package com.buildflow.billing.repository;

import com.buildflow.billing.entity.GstInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GstInvoiceRepository extends JpaRepository<GstInvoice, Long> {
    Optional<GstInvoice> findByRaBillId(Long raBillId);

    boolean existsByRaBillId(Long raBillId);

    Optional<GstInvoice> findByIdAndRaBillProjectBusinessId(Long id, Long businessId);

    List<GstInvoice> findByRaBillProjectBusinessIdOrderByInvoiceDateDesc(Long businessId);

    List<GstInvoice> findByRaBillProjectBusinessIdAndInvoiceDateBetweenOrderByInvoiceDateAsc(
            Long businessId, LocalDate from, LocalDate to);

    long countByRaBillProjectBusinessId(Long businessId);
}
