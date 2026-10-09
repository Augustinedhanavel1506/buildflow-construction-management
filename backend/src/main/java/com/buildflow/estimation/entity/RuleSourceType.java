package com.buildflow.estimation.entity;

/**
 * Where a rule's coefficient came from. Governs how much a caller should trust it — see
 * {@link ConfidenceLevel} and {@code BoqGenerationRule#isVerified()}.
 */
public enum RuleSourceType {
    GOVT_SOR,                  // e.g. Tamil Nadu PWD Schedule of Rates
    GOVT_DSR,                  // e.g. CPWD Delhi Schedule of Rates / material consumption coefficients
    COMPLETED_PROJECT_AVERAGE, // calibrated from this business's own finished projects
    VENDOR_QUOTE,
    PLACEHOLDER                // not sourced yet; must not be trusted for a real quotation
}
