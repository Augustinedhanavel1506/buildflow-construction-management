package com.buildflow.estimation.entity;

import com.buildflow.business.entity.Business;
import com.buildflow.common.entity.BaseEntity;
import com.buildflow.project.entity.Project;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The pre-construction intake: plot + room checklist a landowner enters before a contract
 * exists. Backed 1:1 by a Project created in PLANNING status at the same time, which is what
 * BoqGenerationService writes the generated BoqItems onto.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "house_requirements")
public class HouseRequirement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;

    @Column(nullable = false)
    private String label;

    @Column
    private String location;

    // Optional: selects that district's rates in Rate Master, falling back to the default rates.
    @Column(length = 100)
    private String district;

    // Site rules entered by the user. Null setbacks fall back to planning defaults; null limits are not checked.
    @Column(name = "setback_front_ft", precision = 6, scale = 2)
    private BigDecimal setbackFrontFt;

    @Column(name = "setback_rear_ft", precision = 6, scale = 2)
    private BigDecimal setbackRearFt;

    @Column(name = "setback_left_ft", precision = 6, scale = 2)
    private BigDecimal setbackLeftFt;

    @Column(name = "setback_right_ft", precision = 6, scale = 2)
    private BigDecimal setbackRightFt;

    @Column(name = "max_coverage_percent", precision = 5, scale = 2)
    private BigDecimal maxCoveragePercent;

    @Column(name = "max_far", precision = 5, scale = 2)
    private BigDecimal maxFar;

    // Colour choices of the 3D view as JSON; the shape is owned by the frontend and only size-limited here.
    @Column(name = "house_style", columnDefinition = "text")
    private String houseStyle;

    @Column(name = "plot_width_ft", nullable = false, precision = 10, scale = 2)
    private BigDecimal plotWidthFt;

    @Column(name = "plot_length_ft", nullable = false, precision = 10, scale = 2)
    private BigDecimal plotLengthFt;

    @Enumerated(EnumType.STRING)
    @Column(name = "construction_grade", nullable = false)
    private ConstructionGrade constructionGrade;

    @Enumerated(EnumType.STRING)
    @Column(name = "structure_type", nullable = false)
    private StructureType structureType = StructureType.RCC_FRAMED;

    @Enumerated(EnumType.STRING)
    @Column(name = "wall_material", nullable = false)
    private WallMaterial wallMaterial = WallMaterial.RED_BRICK;

    @Enumerated(EnumType.STRING)
    @Column(name = "roof_type", nullable = false)
    private RoofType roofType = RoofType.RCC_SLAB;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HouseRequirementStatus status = HouseRequirementStatus.DRAFT;

    @OneToMany(mappedBy = "houseRequirement", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("floorLevel asc")
    private List<FloorRequirement> floors = new ArrayList<>();
}
