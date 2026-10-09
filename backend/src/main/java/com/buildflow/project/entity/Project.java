package com.buildflow.project.entity;

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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "projects")
public class Project extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false)
    private String name;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "client_gstin")
    private String clientGstin;

    @Column(name = "client_address", length = 500)
    private String clientAddress;

    @Column
    private String location;

    @Column(name = "contract_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal contractValue;

    @Column(name = "estimated_cost", precision = 15, scale = 2)
    private BigDecimal estimatedCost = BigDecimal.ZERO;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "expected_end_date")
    private LocalDate expectedEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status = ProjectStatus.PLANNING;

    @Column(nullable = false)
    private boolean archived = false;
}
