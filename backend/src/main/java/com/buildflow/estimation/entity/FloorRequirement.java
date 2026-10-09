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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "floor_requirements")
public class FloorRequirement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "house_requirement_id", nullable = false)
    private HouseRequirement houseRequirement;

    // 0 = ground floor, 1 = first floor, etc. Foundation rules key off level 0 specifically.
    @Column(name = "floor_level", nullable = false)
    private int floorLevel;

    @Column(name = "floor_area_sqft", nullable = false, precision = 10, scale = 2)
    private BigDecimal floorAreaSqft;

    @Column(name = "bedroom_count", nullable = false)
    private int bedroomCount = 0;

    @Column(name = "bathroom_count", nullable = false)
    private int bathroomCount = 0;

    @Column(name = "has_kitchen", nullable = false)
    private boolean hasKitchen = false;

    @Column(name = "has_hall", nullable = false)
    private boolean hasHall = false;

    @Column(name = "has_balcony", nullable = false)
    private boolean hasBalcony = false;

    @Column(name = "has_pooja_room", nullable = false)
    private boolean hasPoojaRoom = false;

    @Column(name = "door_count", nullable = false)
    private int doorCount = 0;

    @Column(name = "window_count", nullable = false)
    private int windowCount = 0;
}
