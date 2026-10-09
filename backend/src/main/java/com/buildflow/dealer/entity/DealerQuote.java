package com.buildflow.dealer.entity;

import com.buildflow.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "dealer_quotes")
public class DealerQuote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_request_id", nullable = false)
    private QuoteRequest quoteRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dealer_id", nullable = false)
    private Dealer dealer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DealerQuoteStatus status = DealerQuoteStatus.PENDING;

    @Column(name = "delivery_charge", nullable = false, precision = 15, scale = 2)
    private BigDecimal deliveryCharge = BigDecimal.ZERO;

    @Column(name = "loading_charge", nullable = false, precision = 15, scale = 2)
    private BigDecimal loadingCharge = BigDecimal.ZERO;

    @Column(length = 500)
    private String notes;

    @Column(name = "received_at")
    private Instant receivedAt;

    @OneToMany(mappedBy = "dealerQuote", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("itemName asc")
    private List<DealerQuoteLine> lines = new ArrayList<>();
}
