package com.buildflow.estimation.entity;

import com.buildflow.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * The drawn layout of one floor of a house requirement. Kept apart from FloorRequirement, which
 * is rebuilt whenever the checklist is edited, so editing the checklist does not erase a drawing.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "floor_plans")
public class FloorPlan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "house_requirement_id", nullable = false)
    private HouseRequirement houseRequirement;

    @Column(name = "floor_level", nullable = false)
    private int floorLevel;

    @OneToMany(mappedBy = "floorPlan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id asc")
    private List<PlanRoom> rooms = new ArrayList<>();

    @OneToMany(mappedBy = "floorPlan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id asc")
    private List<PlanOpening> openings = new ArrayList<>();

    @OneToMany(mappedBy = "floorPlan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id asc")
    private List<PlanColumn> columns = new ArrayList<>();

    @OneToMany(mappedBy = "floorPlan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id asc")
    private List<PlanFurniture> furniture = new ArrayList<>();
}
