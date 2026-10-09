package com.buildflow.boq.entity;

import com.buildflow.common.entity.BaseEntity;
import com.buildflow.project.entity.Project;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "boq_items")
public class BoqItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoqCategory category;

    @Column(nullable = false)
    private String unit;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal rate;

    @Column(name = "estimated_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal estimatedAmount;

    @Column(name = "actual_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal actualAmount = BigDecimal.ZERO;

    // Low/high band from the generation rule's min/max coefficient, populated only for
    // SYSTEM_PRELIMINARY items so the UI can show a range instead of a falsely precise figure.
    @Column(name = "quantity_low", precision = 15, scale = 3)
    private BigDecimal quantityLow;

    @Column(name = "quantity_high", precision = 15, scale = 3)
    private BigDecimal quantityHigh;

    @Enumerated(EnumType.STRING)
    @Column(name = "estimate_source", nullable = false)
    private EstimateSource estimateSource = EstimateSource.MANUAL;

    // Plain columns rather than a JPA relationship to BoqGenerationRule, so the boq package does
    // not depend on the estimation package. Enough for traceability without an entity reference.
    @Column(name = "source_rule_id")
    private Long sourceRuleId;

    @Column(name = "source_rule_version")
    private Integer sourceRuleVersion;

    @Column(name = "source_rule_code")
    private String sourceRuleCode;

    // Plain string mirror of the generating rule's BoqComponent (e.g. "FOUNDATION", "MASONRY"),
    // not a JPA enum reference, for the same cross-package reason as the sourceRule* columns above.
    // Null for MANUAL items. Lets the UI group a generated BOQ into a cost-by-component breakdown.
    @Column
    private String component;

    // Engineers are not application users, so a validation records who signed off (as entered by
    // the person recording it) rather than referencing a user account.
    @Column(name = "validated_by")
    private String validatedBy;

    @Column(name = "validated_at")
    private java.time.Instant validatedAt;

    @Column(name = "validation_note", length = 500)
    private String validationNote;
}
