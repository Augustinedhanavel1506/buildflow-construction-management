package com.buildflow.dealer.entity;

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

// One line of a dealer's rate card. itemName matches RateMasterItem / BoqItem names exactly.
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "dealer_rates")
public class DealerRate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dealer_id", nullable = false)
    private Dealer dealer;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(nullable = false)
    private String unit;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal rate;
}
