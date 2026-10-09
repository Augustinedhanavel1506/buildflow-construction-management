package com.buildflow.labour.repository;

import com.buildflow.labour.entity.LabourAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LabourAttendanceRepository extends JpaRepository<LabourAttendance, Long> {
    List<LabourAttendance> findByWorkerProjectIdAndDate(Long projectId, LocalDate date);

    Optional<LabourAttendance> findByWorkerIdAndDate(Long workerId, LocalDate date);

    @Query("""
            select coalesce(sum(w.dailyRate), 0) from LabourAttendance a
            join a.worker w
            where w.project.id = :projectId and a.present = true
            """)
    BigDecimal sumLabourCostByProjectId(@Param("projectId") Long projectId);

    @Query("""
            select w.role, count(a), coalesce(sum(w.dailyRate), 0) from LabourAttendance a
            join a.worker w
            where w.project.id = :projectId and a.present = true
            group by w.role
            order by w.role
            """)
    List<Object[]> findRoleBreakdownByProjectId(@Param("projectId") Long projectId);
}
