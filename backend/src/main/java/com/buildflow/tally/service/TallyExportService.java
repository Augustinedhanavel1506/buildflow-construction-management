package com.buildflow.tally.service;

import com.buildflow.billing.entity.GstInvoice;
import com.buildflow.billing.repository.GstInvoiceRepository;
import com.buildflow.business.entity.Business;
import com.buildflow.business.repository.BusinessRepository;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Produces Tally-compatible "Vouchers" XML (Gateway of Tally &gt; Import Data) so GST tax invoices
 * raised in BuildFlow can be brought into the client's existing Tally books without re-keying them.
 * Debit/credit sign convention follows Tally's own XML schema: ISDEEMEDPOSITIVE=Yes marks a debit and
 * is written as a negative amount; ISDEEMEDPOSITIVE=No marks a credit and is written as positive.
 */
@Service
public class TallyExportService {

    private static final DateTimeFormatter TALLY_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String SALES_LEDGER = "Sales - Construction Services";

    private final GstInvoiceRepository gstInvoiceRepository;
    private final BusinessRepository businessRepository;
    private final CurrentUserProvider currentUserProvider;

    public TallyExportService(GstInvoiceRepository gstInvoiceRepository,
                               BusinessRepository businessRepository,
                               CurrentUserProvider currentUserProvider) {
        this.gstInvoiceRepository = gstInvoiceRepository;
        this.businessRepository = businessRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public String exportSalesVouchersXml(LocalDate from, LocalDate to) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found."));

        List<GstInvoice> invoices = gstInvoiceRepository
                .findByRaBillProjectBusinessIdAndInvoiceDateBetweenOrderByInvoiceDateAsc(businessId, from, to);

        StringBuilder xml = new StringBuilder();
        xml.append("<ENVELOPE>\n");
        xml.append(" <HEADER>\n  <TALLYREQUEST>Import Data</TALLYREQUEST>\n </HEADER>\n");
        xml.append(" <BODY>\n  <IMPORTDATA>\n   <REQUESTDESC>\n    <REPORTNAME>Vouchers</REPORTNAME>\n");
        xml.append("    <STATICVARIABLES>\n     <SVCURRENTCOMPANY>").append(escape(business.getName()))
                .append("</SVCURRENTCOMPANY>\n    </STATICVARIABLES>\n   </REQUESTDESC>\n");
        xml.append("   <REQUESTDATA>\n");
        for (GstInvoice invoice : invoices) {
            xml.append(buildSalesVoucher(invoice));
        }
        xml.append("   </REQUESTDATA>\n  </IMPORTDATA>\n </BODY>\n</ENVELOPE>\n");
        return xml.toString();
    }

    private String buildSalesVoucher(GstInvoice invoice) {
        String partyLedger = invoice.getClientName() != null && !invoice.getClientName().isBlank()
                ? invoice.getClientName()
                : invoice.getRaBill().getProject().getName();

        StringBuilder v = new StringBuilder();
        v.append("    <TALLYMESSAGE xmlns:UDF=\"TallyUDF\">\n");
        v.append("     <VOUCHER VCHTYPE=\"Sales\" ACTION=\"Create\">\n");
        v.append("      <DATE>").append(invoice.getInvoiceDate().format(TALLY_DATE)).append("</DATE>\n");
        v.append("      <VOUCHERTYPENAME>Sales</VOUCHERTYPENAME>\n");
        v.append("      <VOUCHERNUMBER>").append(escape(invoice.getInvoiceNumber())).append("</VOUCHERNUMBER>\n");
        v.append("      <PARTYLEDGERNAME>").append(escape(partyLedger)).append("</PARTYLEDGERNAME>\n");
        v.append("      <NARRATION>").append(escape(invoice.getDescription())).append("</NARRATION>\n");

        v.append(ledgerEntry(partyLedger, true, invoice.getTotalInvoiceValue()));
        v.append(ledgerEntry(SALES_LEDGER, false, invoice.getTaxableValue()));
        if (invoice.getCgstAmount().signum() > 0) {
            v.append(ledgerEntry("Output CGST", false, invoice.getCgstAmount()));
        }
        if (invoice.getSgstAmount().signum() > 0) {
            v.append(ledgerEntry("Output SGST", false, invoice.getSgstAmount()));
        }
        if (invoice.getIgstAmount().signum() > 0) {
            v.append(ledgerEntry("Output IGST", false, invoice.getIgstAmount()));
        }

        v.append("     </VOUCHER>\n");
        v.append("    </TALLYMESSAGE>\n");
        return v.toString();
    }

    private String ledgerEntry(String ledgerName, boolean isDebit, BigDecimal amount) {
        BigDecimal signedAmount = (isDebit ? amount.negate() : amount).setScale(2, RoundingMode.HALF_UP);
        return "      <ALLLEDGERENTRIES.LIST>\n"
                + "       <LEDGERNAME>" + escape(ledgerName) + "</LEDGERNAME>\n"
                + "       <ISDEEMEDPOSITIVE>" + (isDebit ? "Yes" : "No") + "</ISDEEMEDPOSITIVE>\n"
                + "       <AMOUNT>" + signedAmount + "</AMOUNT>\n"
                + "      </ALLLEDGERENTRIES.LIST>\n";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
