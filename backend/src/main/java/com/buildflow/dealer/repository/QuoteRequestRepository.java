package com.buildflow.dealer.repository;

import com.buildflow.dealer.entity.QuoteRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuoteRequestRepository extends JpaRepository<QuoteRequest, Long> {
    List<QuoteRequest> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

    Optional<QuoteRequest> findByIdAndBusinessId(Long id, Long businessId);
}
