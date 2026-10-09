package com.buildflow.estimation.entity;

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

/** A piece of furniture or fitting placed on a floor. Layout only: it never changes the estimate. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "plan_furniture")
public class PlanFurniture extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_plan_id", nullable = false)
    private FloorPlan floorPlan;

    @Column(name = "client_id", nullable = false, length = 40)
    private String clientId;

    // Kept as text, validated against a fixed list, so adding a kind never needs a column migration.
    @Column(nullable = false, length = 30)
    private String kind;

    // Feet, the piece's centre measured from the plot's top-left corner.
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal x;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal y;

    @Column(name = "width_ft", nullable = false, precision = 6, scale = 2)
    private BigDecimal widthFt;

    @Column(name = "depth_ft", nullable = false, precision = 6, scale = 2)
    private BigDecimal depthFt;

    @Column(name = "height_ft", nullable = false, precision = 6, scale = 2)
    private BigDecimal heightFt;

    // 0, 90, 180 or 270 degrees.
    @Column(name = "rotation_deg", nullable = false)
    private int rotationDeg;

    @Column(length = 9)
    private String colour;
}
