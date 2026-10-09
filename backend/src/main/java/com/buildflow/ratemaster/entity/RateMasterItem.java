package com.buildflow.ratemaster.entity;

import com.buildflow.boq.entity.BoqCategory;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rate_master_items")
public class RateMasterItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoqCategory category;

    @Column(nullable = false)
    private String unit;

    @Column(name = "standard_rate", nullable = false, precision = 15, scale = 2)
    private BigDecimal standardRate;

    // Optional market price band (e.g. ₹370–₹411/bag) so builders quoting off this rate see the
    // realistic spread instead of trusting a single rigid figure that goes stale within weeks.
    @Column(name = "min_rate", precision = 15, scale = 2)
    private BigDecimal minRate;

    @Column(name = "max_rate", precision = 15, scale = 2)
    private BigDecimal maxRate;

    // Quantity of this item's unit typically consumed per sqft of built-up area (thumb-rule
    // coefficient), used by the material quantity estimator. Meaningful mainly for MATERIAL items.
    @Column(name = "consumption_per_sqft", precision = 10, scale = 4)
    private BigDecimal consumptionPerSqft;

    // Null means the business's default rate; a value makes it the rate for that district only.
    @Column(length = 100)
    private String district;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private boolean active = true;
}
