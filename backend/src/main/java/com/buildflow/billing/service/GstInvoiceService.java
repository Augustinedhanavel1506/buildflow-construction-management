package com.buildflow.billing.service;

import com.buildflow.billing.dto.GstInvoiceRequest;
import com.buildflow.billing.dto.GstInvoiceResponse;
import com.buildflow.billing.entity.GstInvoice;
import com.buildflow.billing.entity.RaBill;
import com.buildflow.billing.entity.RaBillStatus;
import com.buildflow.billing.repository.GstInvoiceRepository;
import com.buildflow.business.entity.Business;
import com.buildflow.common.exception.BadRequestException;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.project.entity.Project;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class GstInvoiceService {

    private static final BigDecimal DEFAULT_GST_RATE = new BigDecimal("18.00");
    private static final String DEFAULT_HSN_SAC = "9954";

    private final GstInvoiceRepository gstInvoiceRepository;
    private final RaBillService raBillService;
    private final CurrentUserProvider currentUserProvider;

    public GstInvoiceService(GstInvoiceRepository gstInvoiceRepository,
                              RaBillService raBillService,
                              CurrentUserProvider currentUserProvider) {
        this.gstInvoiceRepository = gstInvoiceRepository;
        this.raBillService = raBillService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public GstInvoiceResponse get(Long id) {
        return GstInvoiceResponse.from(findOwnedInvoice(id));
    }

    @Transactional(readOnly = true)
    public GstInvoiceResponse getForBill(Long raBillId) {
        raBillService.findOwnedBillForRead(raBillId);
        GstInvoice invoice = gstInvoiceRepository.findByRaBillId(raBillId)
                .orElseThrow(() -> new ResourceNotFoundException("No GST invoice has been issued for this bill yet."));
        return GstInvoiceResponse.from(invoice);
    }

    @Transactional(readOnly = true)
    public List<GstInvoiceResponse> list() {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return gstInvoiceRepository.findByRaBillProjectBusinessIdOrderByInvoiceDateDesc(businessId).stream()
                .map(GstInvoiceResponse::from)
                .toList();
    }

    @Transactional
    public GstInvoiceResponse create(Long raBillId, GstInvoiceRequest request) {
        RaBill bill = raBillService.findOwnedBillForRead(raBillId);

        if (bill.getStatus() == RaBillStatus.DRAFT) {
            throw new BadRequestException("Submit this bill before issuing a tax invoice against it.");
        }
        if (gstInvoiceRepository.existsByRaBillId(raBillId)) {
            throw new BadRequestException("A tax invoice has already been issued for this bill.");
        }

        Project project = bill.getProject();
        Business business = project.getBusiness();

        BigDecimal taxableValue = bill.getCertifiedAmount() != null ? bill.getCertifiedAmount() : bill.getWorkDoneValue();
        BigDecimal gstRate = request.gstRate() != null
                ? request.gstRate()
                : (business.getDefaultGstRate() != null ? business.getDefaultGstRate() : DEFAULT_GST_RATE);

        GstInvoice invoice = new GstInvoice();
        invoice.setRaBill(bill);
        invoice.setInvoiceNumber(request.invoiceNumber());
        invoice.setInvoiceDate(request.invoiceDate());
        invoice.setHsnSacCode(request.hsnSacCode() != null && !request.hsnSacCode().isBlank() ? request.hsnSacCode() : DEFAULT_HSN_SAC);
        invoice.setDescription(request.description() != null && !request.description().isBlank()
                ? request.description()
                : "Construction services for %s — RA Bill %s".formatted(project.getName(), bill.getBillNumber()));
        invoice.setTaxableValue(taxableValue);
        invoice.setGstRate(gstRate);
        invoice.setInterState(request.interState());
        invoice.setPlaceOfSupply(request.placeOfSupply());

        applyTaxBreakup(invoice, taxableValue, gstRate, request.interState());

        invoice.setSupplierName(business.getName());
        invoice.setSupplierGstin(business.getGstin());
        invoice.setSupplierAddress(business.getAddress());
        invoice.setClientName(project.getClientName());
        invoice.setClientGstin(project.getClientGstin());
        invoice.setClientAddress(project.getClientAddress());
        invoice.setCreatedBy(currentUserProvider.getCurrentUser());

        return GstInvoiceResponse.from(gstInvoiceRepository.save(invoice));
    }

    private void applyTaxBreakup(GstInvoice invoice, BigDecimal taxableValue, BigDecimal gstRate, boolean interState) {
        BigDecimal totalTax = taxableValue.multiply(gstRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        if (interState) {
            invoice.setIgstAmount(totalTax);
            invoice.setCgstAmount(BigDecimal.ZERO);
            invoice.setSgstAmount(BigDecimal.ZERO);
        } else {
            BigDecimal half = totalTax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            invoice.setCgstAmount(half);
            invoice.setSgstAmount(half);
            invoice.setIgstAmount(BigDecimal.ZERO);
        }

        invoice.setTotalInvoiceValue(taxableValue.add(invoice.getCgstAmount()).add(invoice.getSgstAmount()).add(invoice.getIgstAmount()));
    }

    private GstInvoice findOwnedInvoice(Long id) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return gstInvoiceRepository.findByIdAndRaBillProjectBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("GST invoice not found."));
    }
}
