package com.buildflow.progress.repository;

import com.buildflow.progress.entity.ProjectProgressStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectProgressStageRepository extends JpaRepository<ProjectProgressStage, Long> {
    List<ProjectProgressStage> findByProjectIdOrderBySortOrderAsc(Long projectId);

    Optional<ProjectProgressStage> findByIdAndProjectBusinessId(Long id, Long businessId);

    @Query("select coalesce(avg(s.percentComplete), 0) from ProjectProgressStage s where s.project.id = :projectId")
    double averagePercentCompleteByProjectId(@Param("projectId") Long projectId);

    @Query("select coalesce(max(s.sortOrder), 0) from ProjectProgressStage s where s.project.id = :projectId")
    int maxSortOrderByProjectId(@Param("projectId") Long projectId);
}
