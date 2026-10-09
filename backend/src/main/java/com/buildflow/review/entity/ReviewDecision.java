package com.buildflow.review.entity;

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

// The engineer's call on one line, kept as a record of what was changed and why even if the BOQ
// line is edited later.
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "review_decisions")
public class ReviewDecision extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_request_id", nullable = false)
    private ReviewRequest reviewRequest;

    @Column(name = "boq_item_id", nullable = false)
    private Long boqItemId;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "original_quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal originalQuantity;

    @Column(name = "corrected_quantity", precision = 15, scale = 3)
    private BigDecimal correctedQuantity;

    @Column(length = 500)
    private String comment;
}
