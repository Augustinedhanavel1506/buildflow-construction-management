package com.buildflow.expense.repository;

import com.buildflow.expense.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByProjectIdOrderByDateDesc(Long projectId);

    Optional<Expense> findByIdAndProjectBusinessId(Long id, Long businessId);

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.project.id = :projectId")
    BigDecimal sumAmountByProjectId(@Param("projectId") Long projectId);

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.project.business.id = :businessId")
    BigDecimal sumAmountByBusinessId(@Param("businessId") Long businessId);

    @Query("""
            select e.category, coalesce(sum(e.amount), 0) from Expense e
            where e.project.business.id = :businessId
            group by e.category
            """)
    List<Object[]> findCategoryBreakdownByBusinessId(@Param("businessId") Long businessId);

    @Query("""
            select e.category, coalesce(sum(e.amount), 0) from Expense e
            where e.project.id = :projectId
            group by e.category
            """)
    List<Object[]> findCategoryBreakdownByProjectId(@Param("projectId") Long projectId);

    List<Expense> findTop5ByProjectBusinessIdOrderByCreatedAtDesc(Long businessId);
}
