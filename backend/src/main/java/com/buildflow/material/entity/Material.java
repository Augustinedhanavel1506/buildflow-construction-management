package com.buildflow.material.entity;

import com.buildflow.common.entity.BaseEntity;
import com.buildflow.project.entity.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "materials")
public class Material extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String unit;

    @Column(name = "minimum_stock", nullable = false, precision = 15, scale = 3)
    private BigDecimal minimumStock;

    @Column(name = "current_stock", nullable = false, precision = 15, scale = 3)
    private BigDecimal currentStock;
}
