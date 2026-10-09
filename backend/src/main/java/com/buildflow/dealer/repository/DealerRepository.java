package com.buildflow.dealer.repository;

import com.buildflow.dealer.entity.Dealer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DealerRepository extends JpaRepository<Dealer, Long> {
    List<Dealer> findByBusinessIdOrderByNameAsc(Long businessId);

    Optional<Dealer> findByIdAndBusinessId(Long id, Long businessId);
}
