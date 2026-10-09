package com.buildflow.billing.entity;

import com.buildflow.auth.entity.User;
import com.buildflow.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "gst_invoices")
public class GstInvoice extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ra_bill_id", nullable = false, unique = true)
    private RaBill raBill;

    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "hsn_sac_code", nullable = false)
    private String hsnSacCode;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "taxable_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxableValue;

    @Column(name = "gst_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal gstRate;

    @Column(name = "is_inter_state", nullable = false)
    private boolean interState;

    @Column(name = "cgst_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal cgstAmount;

    @Column(name = "sgst_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal sgstAmount;

    @Column(name = "igst_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal igstAmount;

    @Column(name = "total_invoice_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalInvoiceValue;

    @Column(name = "place_of_supply")
    private String placeOfSupply;

    // Supplier and client details are snapshotted at issuance so a later edit to the
    // business profile or project's client details never silently rewrites a past invoice.
    @Column(name = "supplier_name", nullable = false)
    private String supplierName;

    @Column(name = "supplier_gstin")
    private String supplierGstin;

    @Column(name = "supplier_address", length = 500)
    private String supplierAddress;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "client_gstin")
    private String clientGstin;

    @Column(name = "client_address", length = 500)
    private String clientAddress;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;
}
