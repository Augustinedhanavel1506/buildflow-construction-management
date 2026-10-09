package com.buildflow.dealer.entity;

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
@Table(name = "dealer_quote_lines")
public class DealerQuoteLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dealer_quote_id", nullable = false)
    private DealerQuote dealerQuote;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    // Null means this dealer has not priced the item (does not stock it, or has not answered).
    @Column(name = "unit_rate", precision = 15, scale = 2)
    private BigDecimal unitRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuoteLineSource source = QuoteLineSource.INDICATIVE;
}
