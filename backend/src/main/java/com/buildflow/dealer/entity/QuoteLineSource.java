package com.buildflow.dealer.entity;

public enum QuoteLineSource {
    // Prefilled from the dealer's rate card; not a real quotation yet.
    INDICATIVE,
    // Entered from the dealer's actual response.
    QUOTED
}
