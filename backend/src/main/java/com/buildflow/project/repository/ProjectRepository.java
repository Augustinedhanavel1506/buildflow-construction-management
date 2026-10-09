package com.buildflow.project.repository;

import com.buildflow.project.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Page<Project> findByBusinessIdAndArchivedFalse(Long businessId, Pageable pageable);

    List<Project> findByBusinessIdAndArchivedFalse(Long businessId);

    Optional<Project> findByIdAndBusinessId(Long id, Long businessId);
}
