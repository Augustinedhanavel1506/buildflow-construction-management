package com.buildflow.billing.service;

import com.buildflow.billing.dto.BillingSummaryResponse;
import com.buildflow.billing.dto.CertifyBillRequest;
import com.buildflow.billing.dto.RaBillRequest;
import com.buildflow.billing.dto.RaBillResponse;
import com.buildflow.billing.entity.RaBill;
import com.buildflow.billing.entity.RaBillStatus;
import com.buildflow.billing.repository.BillPaymentRepository;
import com.buildflow.billing.repository.RaBillRepository;
import com.buildflow.common.exception.BadRequestException;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.project.entity.Project;
import com.buildflow.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class RaBillService {

    private final RaBillRepository raBillRepository;
    private final BillPaymentRepository billPaymentRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserProvider currentUserProvider;

    public RaBillService(RaBillRepository raBillRepository,
                          BillPaymentRepository billPaymentRepository,
                          ProjectRepository projectRepository,
                          CurrentUserProvider currentUserProvider) {
        this.raBillRepository = raBillRepository;
        this.billPaymentRepository = billPaymentRepository;
        this.projectRepository = projectRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<RaBillResponse> list(Long projectId) {
        Project project = findOwnedProject(projectId);
        return raBillRepository.findByProjectIdOrderByBillDateDesc(project.getId()).stream()
                .map(bill -> RaBillResponse.from(bill, billPaymentRepository.sumAmountByRaBillId(bill.getId())))
                .toList();
    }

    @Transactional
    public RaBillResponse create(Long projectId, RaBillRequest request) {
        Project project = findOwnedProject(projectId);

        RaBill bill = new RaBill();
        bill.setProject(project);
        bill.setCreatedBy(currentUserProvider.getCurrentUser());
        applyDraftFields(bill, request);

        RaBill saved = raBillRepository.save(bill);
        return RaBillResponse.from(saved, BigDecimal.ZERO);
    }

    @Transactional
    public RaBillResponse update(Long id, RaBillRequest request) {
        RaBill bill = findOwnedBill(id);
        if (bill.getStatus() != RaBillStatus.DRAFT) {
            throw new BadRequestException("Only draft bills can be edited.");
        }
        applyDraftFields(bill, request);
        RaBill saved = raBillRepository.save(bill);
        return RaBillResponse.from(saved, BigDecimal.ZERO);
    }

    @Transactional
    public RaBillResponse submit(Long id) {
        RaBill bill = findOwnedBill(id);
        if (bill.getStatus() != RaBillStatus.DRAFT) {
            throw new BadRequestException("Only draft bills can be submitted.");
        }
        bill.setStatus(RaBillStatus.SUBMITTED);
        RaBill saved = raBillRepository.save(bill);
        return RaBillResponse.from(saved, billPaymentRepository.sumAmountByRaBillId(saved.getId()));
    }

    @Transactional
    public RaBillResponse certify(Long id, CertifyBillRequest request) {
        RaBill bill = findOwnedBill(id);
        if (bill.getStatus() != RaBillStatus.SUBMITTED) {
            throw new BadRequestException("Only submitted bills can be certified.");
        }
        bill.setCertifiedAmount(request.certifiedAmount());
        recomputeAmounts(bill);
        bill.setStatus(RaBillStatus.CERTIFIED);
        RaBill saved = raBillRepository.save(bill);
        return RaBillResponse.from(saved, billPaymentRepository.sumAmountByRaBillId(saved.getId()));
    }

    @Transactional(readOnly = true)
    public BillingSummaryResponse getSummary(Long projectId) {
        Project project = findOwnedProject(projectId);
        BigDecimal totalBilled = raBillRepository.sumNetPayableByProjectId(project.getId(), RaBillStatus.DRAFT);
        BigDecimal totalReceived = billPaymentRepository.sumAmountByProjectId(project.getId());
        BigDecimal totalRetentionHeld = raBillRepository.sumRetentionByProjectId(project.getId(), RaBillStatus.DRAFT);
        return new BillingSummaryResponse(totalBilled, totalReceived, totalBilled.subtract(totalReceived), totalRetentionHeld);
    }

    @Transactional(readOnly = true)
    public BillingSummaryResponse getSummaryForBusiness(Long businessId) {
        BigDecimal totalBilled = raBillRepository.sumNetPayableByBusinessId(businessId, RaBillStatus.DRAFT);
        BigDecimal totalReceived = billPaymentRepository.sumAmountByBusinessId(businessId);
        BigDecimal totalRetentionHeld = raBillRepository.sumRetentionByBusinessId(businessId, RaBillStatus.DRAFT);
        return new BillingSummaryResponse(totalBilled, totalReceived, totalBilled.subtract(totalReceived), totalRetentionHeld);
    }

    RaBill findOwnedBillForRead(Long id) {
        return findOwnedBill(id);
    }

    RaBill findOwnedBillForPayment(Long id) {
        RaBill bill = findOwnedBill(id);
        if (bill.getStatus() == RaBillStatus.DRAFT) {
            throw new BadRequestException("Submit this bill before recording payments against it.");
        }
        if (bill.getStatus() == RaBillStatus.PAID) {
            throw new BadRequestException("This bill has already been fully paid.");
        }
        return bill;
    }

    @Transactional
    void markPaidIfFullyReceived(RaBill bill, BigDecimal totalReceived) {
        if (totalReceived.compareTo(bill.getNetPayableAmount()) >= 0) {
            bill.setStatus(RaBillStatus.PAID);
            raBillRepository.save(bill);
        }
    }

    private void applyDraftFields(RaBill bill, RaBillRequest request) {
        bill.setBillNumber(request.billNumber());
        bill.setBillDate(request.billDate());
        bill.setWorkDoneValue(request.workDoneValue());
        bill.setRetentionPercent(request.retentionPercent());
        bill.setOtherDeductions(request.otherDeductions() != null ? request.otherDeductions() : BigDecimal.ZERO);
        bill.setNotes(request.notes());
        recomputeAmounts(bill);
    }

    private void recomputeAmounts(RaBill bill) {
        BigDecimal effectiveValue = bill.getCertifiedAmount() != null ? bill.getCertifiedAmount() : bill.getWorkDoneValue();
        BigDecimal retentionAmount = effectiveValue
                .multiply(bill.getRetentionPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        bill.setRetentionAmount(retentionAmount);
        bill.setNetPayableAmount(effectiveValue.subtract(retentionAmount).subtract(bill.getOtherDeductions()));
    }

    private Project findOwnedProject(Long projectId) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return projectRepository.findByIdAndBusinessId(projectId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
    }

    private RaBill findOwnedBill(Long id) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return raBillRepository.findByIdAndProjectBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("RA bill not found."));
    }
}
