package com.buildflow.ratemaster.repository;

import com.buildflow.ratemaster.entity.RateMasterItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RateMasterItemRepository extends JpaRepository<RateMasterItem, Long> {
    List<RateMasterItem> findByBusinessIdOrderByItemNameAsc(Long businessId);

    List<RateMasterItem> findByBusinessIdAndActiveTrueOrderByItemNameAsc(Long businessId);

    Optional<RateMasterItem> findByIdAndBusinessId(Long id, Long businessId);
}
