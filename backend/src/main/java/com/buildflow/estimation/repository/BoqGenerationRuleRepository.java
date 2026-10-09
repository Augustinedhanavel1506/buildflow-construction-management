package com.buildflow.estimation.repository;

import com.buildflow.estimation.entity.BoqGenerationRule;
import com.buildflow.estimation.entity.ConstructionGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BoqGenerationRuleRepository extends JpaRepository<BoqGenerationRule, Long> {

    @Query("""
            select r from BoqGenerationRule r
            where r.business is null or r.business.id = :businessId
            order by r.component, r.itemName, r.constructionGrade
            """)
    List<BoqGenerationRule> findVisibleToBusiness(@Param("businessId") Long businessId);

    Optional<BoqGenerationRule> findByRuleCode(String ruleCode);

    // Structure/wall/roof/floor-range matching happens in BoqGenerationService (small, config-sized
    // result set) rather than here, since those are nullable "wildcard" columns that are awkward to
    // express as a single JPQL predicate.
    @Query("""
            select r from BoqGenerationRule r
            where r.active = true
              and r.constructionGrade = :grade
              and r.effectiveFrom <= :today
              and (r.effectiveTo is null or r.effectiveTo >= :today)
              and (r.business is null or r.business.id = :businessId)
            """)
    List<BoqGenerationRule> findApplicable(
            @Param("grade") ConstructionGrade grade,
            @Param("today") LocalDate today,
            @Param("businessId") Long businessId);
}
