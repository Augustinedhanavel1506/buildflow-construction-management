package com.buildflow.estimation.entity;

import com.buildflow.boq.entity.BoqCategory;
import com.buildflow.business.entity.Business;
import com.buildflow.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A configurable thumb-rule coefficient used by BoqGenerationService to turn a
 * {@link com.buildflow.estimation.entity.HouseRequirement} into preliminary {@link
 * com.buildflow.boq.entity.BoqItem} quantities. Deliberately data, not code, so an admin can
 * add/edit rules without a redeploy.
 *
 * business == null means a platform-wide default rule; a non-null business overrides the
 * platform default for the same ruleCode (see BoqGenerationRuleRepository.findApplicable).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "boq_generation_rules")
public class BoqGenerationRule extends BaseEntity {

    @Column(name = "rule_code", nullable = false, unique = true)
    private String ruleCode;

    // Platform rule this row overrides (equals ruleCode for platform rules). Null on rows created
    // before overrides existed; treat as ruleCode.
    @Column(name = "base_rule_code")
    private String baseRuleCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoqComponent component;

    @Enumerated(EnumType.STRING)
    @Column(name = "construction_grade", nullable = false)
    private ConstructionGrade constructionGrade;

    // Should match RateMasterItem.itemName for the same business so the pricing engine can join
    // a generated quantity to a rate without an extra mapping table.
    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(name = "boq_category", nullable = false)
    private BoqCategory boqCategory;

    @Column(nullable = false)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstimationBasis basis;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal coefficient;

    @Column(name = "wastage_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal wastagePercent = BigDecimal.ZERO;

    // Calibration band the point-estimate coefficient was drawn from. Drives quantityLow/High on
    // the generated BoqItem so the UI shows a range instead of a falsely precise number.
    @Column(name = "min_coefficient", precision = 10, scale = 4)
    private BigDecimal minCoefficient;

    @Column(name = "max_coefficient", precision = 10, scale = 4)
    private BigDecimal maxCoefficient;

    // Null on any of these three = applies regardless (wildcard). Lets the seed set ship without
    // committing every rule to a specific structure/wall/roof combination up front.
    @Enumerated(EnumType.STRING)
    @Column(name = "structure_type")
    private StructureType structureType;

    @Enumerated(EnumType.STRING)
    @Column(name = "wall_material")
    private WallMaterial wallMaterial;

    @Enumerated(EnumType.STRING)
    @Column(name = "roof_type")
    private RoofType roofType;

    @Column(name = "applicable_floor_min")
    private Integer applicableFloorMin;

    @Column(name = "applicable_floor_max")
    private Integer applicableFloorMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private RuleSourceType sourceType;

    @Column(name = "source_reference", nullable = false, length = 500)
    private String sourceReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_level", nullable = false)
    private ConfidenceLevel confidenceLevel = ConfidenceLevel.LOW;

    @Column(nullable = false)
    private boolean verified = false;

    // Number of completed projects the coefficient was averaged from, when sourceType is
    // COMPLETED_PROJECT_AVERAGE. Null for government-schedule or placeholder rules.
    @Column(name = "sample_size")
    private Integer sampleSize;

    @Column
    private String region;

    @Column(nullable = false)
    private int version = 1;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id")
    private Business business;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 500)
    private String notes;
}
