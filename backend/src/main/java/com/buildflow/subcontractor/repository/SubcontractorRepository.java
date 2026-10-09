package com.buildflow.subcontractor.repository;

import com.buildflow.subcontractor.entity.Subcontractor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubcontractorRepository extends JpaRepository<Subcontractor, Long> {
    List<Subcontractor> findByBusinessIdOrderByNameAsc(Long businessId);

    List<Subcontractor> findByBusinessIdAndActiveTrueOrderByNameAsc(Long businessId);

    Optional<Subcontractor> findByIdAndBusinessId(Long id, Long businessId);
}
