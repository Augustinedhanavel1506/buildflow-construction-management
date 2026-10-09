package com.buildflow.estimation.entity;

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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "plan_openings")
public class PlanOpening extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_plan_id", nullable = false)
    private FloorPlan floorPlan;

    @Column(name = "client_id", nullable = false, length = 40)
    private String clientId;

    @Column(name = "room_client_id", nullable = false, length = 40)
    private String roomClientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpeningKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanSide side;

    // Distance in feet along the side, from its start: left edge for NORTH/SOUTH, top edge for EAST/WEST.
    @Column(name = "offset_ft", nullable = false, precision = 8, scale = 2)
    private BigDecimal offsetFt;

    @Column(name = "width_ft", nullable = false, precision = 8, scale = 2)
    private BigDecimal widthFt;
}
