package com.buildflow.material.repository;

import com.buildflow.material.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findByProjectIdOrderByNameAsc(Long projectId);

    List<Material> findByProjectBusinessId(Long businessId);

    Optional<Material> findByIdAndProjectBusinessId(Long id, Long businessId);
}
