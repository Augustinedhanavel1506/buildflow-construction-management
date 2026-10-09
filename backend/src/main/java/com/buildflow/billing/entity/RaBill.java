package com.buildflow.billing.entity;

import com.buildflow.auth.entity.User;
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
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ra_bills")
public class RaBill extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "bill_number", nullable = false)
    private String billNumber;

    @Column(name = "bill_date", nullable = false)
    private LocalDate billDate;

    @Column(name = "work_done_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal workDoneValue;

    @Column(name = "retention_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal retentionPercent;

    @Column(name = "other_deductions", nullable = false, precision = 15, scale = 2)
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    @Column(name = "certified_amount", precision = 15, scale = 2)
    private BigDecimal certifiedAmount;

    @Column(name = "retention_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal retentionAmount;

    @Column(name = "net_payable_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal netPayableAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RaBillStatus status = RaBillStatus.DRAFT;

    @Column(length = 1000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;
}
