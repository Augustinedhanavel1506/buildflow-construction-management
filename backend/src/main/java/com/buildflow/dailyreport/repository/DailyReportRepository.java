package com.buildflow.dailyreport.repository;

import com.buildflow.dailyreport.entity.DailyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {
    List<DailyReport> findByProjectIdOrderByReportDateDesc(Long projectId);

    Optional<DailyReport> findByIdAndProjectBusinessId(Long id, Long businessId);

    List<DailyReport> findTop5ByProjectBusinessIdOrderByCreatedAtDesc(Long businessId);
}
