package com.buildflow.material.entity;

import com.buildflow.auth.entity.User;
import com.buildflow.common.entity.BaseEntity;
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
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "material_stock_counts")
public class MaterialStockCount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "count_date", nullable = false)
    private LocalDate countDate;

    @Column(name = "system_stock", nullable = false, precision = 15, scale = 3)
    private BigDecimal systemStock;

    @Column(name = "counted_stock", nullable = false, precision = 15, scale = 3)
    private BigDecimal countedStock;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal variance;

    @Column(length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "counted_by_user_id", nullable = false)
    private User countedBy;
}
