package com.buildflow.subcontractor.service;

import com.buildflow.common.exception.BadRequestException;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.subcontractor.dto.SubcontractorBillRequest;
import com.buildflow.subcontractor.dto.SubcontractorBillResponse;
import com.buildflow.subcontractor.entity.SubcontractWorkOrder;
import com.buildflow.subcontractor.entity.SubcontractorBill;
import com.buildflow.subcontractor.entity.SubcontractorBillStatus;
import com.buildflow.subcontractor.repository.SubcontractorBillRepository;
import com.buildflow.subcontractor.repository.SubcontractorPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class SubcontractorBillService {

    private final SubcontractorBillRepository subcontractorBillRepository;
    private final SubcontractorPaymentRepository subcontractorPaymentRepository;
    private final SubcontractWorkOrderService workOrderService;
    private final CurrentUserProvider currentUserProvider;

    public SubcontractorBillService(SubcontractorBillRepository subcontractorBillRepository,
                                     SubcontractorPaymentRepository subcontractorPaymentRepository,
                                     SubcontractWorkOrderService workOrderService,
                                     CurrentUserProvider currentUserProvider) {
        this.subcontractorBillRepository = subcontractorBillRepository;
        this.subcontractorPaymentRepository = subcontractorPaymentRepository;
        this.workOrderService = workOrderService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<SubcontractorBillResponse> list(Long workOrderId) {
        workOrderService.findOwnedWorkOrder(workOrderId);
        return subcontractorBillRepository.findByWorkOrderIdOrderByBillDateDesc(workOrderId).stream()
                .map(bill -> SubcontractorBillResponse.from(bill, subcontractorPaymentRepository.sumAmountBySubcontractorBillId(bill.getId())))
                .toList();
    }

    @Transactional
    public SubcontractorBillResponse create(Long workOrderId, SubcontractorBillRequest request) {
        SubcontractWorkOrder workOrder = workOrderService.findOwnedWorkOrder(workOrderId);

        SubcontractorBill bill = new SubcontractorBill();
        bill.setWorkOrder(workOrder);
        bill.setCreatedBy(currentUserProvider.getCurrentUser());
        applyDraftFields(bill, request);

        return SubcontractorBillResponse.from(subcontractorBillRepository.save(bill), BigDecimal.ZERO);
    }

    @Transactional
    public SubcontractorBillResponse update(Long id, SubcontractorBillRequest request) {
        SubcontractorBill bill = findOwnedBill(id);
        if (bill.getStatus() != SubcontractorBillStatus.DRAFT) {
            throw new BadRequestException("Only draft bills can be edited.");
        }
        applyDraftFields(bill, request);
        return SubcontractorBillResponse.from(subcontractorBillRepository.save(bill), BigDecimal.ZERO);
    }

    @Transactional
    public SubcontractorBillResponse approve(Long id) {
        SubcontractorBill bill = findOwnedBill(id);
        if (bill.getStatus() != SubcontractorBillStatus.DRAFT) {
            throw new BadRequestException("Only draft bills can be approved.");
        }
        bill.setStatus(SubcontractorBillStatus.APPROVED);
        SubcontractorBill saved = subcontractorBillRepository.save(bill);
        return SubcontractorBillResponse.from(saved, subcontractorPaymentRepository.sumAmountBySubcontractorBillId(saved.getId()));
    }

    SubcontractorBill findOwnedBillForRead(Long id) {
        return findOwnedBill(id);
    }

    SubcontractorBill findOwnedBillForPayment(Long id) {
        SubcontractorBill bill = findOwnedBill(id);
        if (bill.getStatus() == SubcontractorBillStatus.DRAFT) {
            throw new BadRequestException("Approve this bill before recording payments against it.");
        }
        if (bill.getStatus() == SubcontractorBillStatus.PAID) {
            throw new BadRequestException("This bill has already been fully paid.");
        }
        return bill;
    }

    @Transactional
    void markPaidIfFullyReceived(SubcontractorBill bill, BigDecimal totalPaid) {
        if (totalPaid.compareTo(bill.getNetPayableAmount()) >= 0) {
            bill.setStatus(SubcontractorBillStatus.PAID);
            subcontractorBillRepository.save(bill);
        }
    }

    private void applyDraftFields(SubcontractorBill bill, SubcontractorBillRequest request) {
        bill.setBillNumber(request.billNumber());
        bill.setBillDate(request.billDate());
        bill.setWorkDoneValue(request.workDoneValue());
        bill.setTdsPercent(request.tdsPercent());
        bill.setRetentionPercent(request.retentionPercent());
        bill.setOtherDeductions(request.otherDeductions() != null ? request.otherDeductions() : BigDecimal.ZERO);
        bill.setNotes(request.notes());
        recomputeAmounts(bill);
    }

    private void recomputeAmounts(SubcontractorBill bill) {
        BigDecimal tdsAmount = bill.getWorkDoneValue()
                .multiply(bill.getTdsPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal retentionAmount = bill.getWorkDoneValue()
                .multiply(bill.getRetentionPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        bill.setTdsAmount(tdsAmount);
        bill.setRetentionAmount(retentionAmount);
        bill.setNetPayableAmount(bill.getWorkDoneValue().subtract(tdsAmount).subtract(retentionAmount).subtract(bill.getOtherDeductions()));
    }

    private SubcontractorBill findOwnedBill(Long id) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return subcontractorBillRepository.findByIdAndWorkOrderProjectBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Subcontractor bill not found."));
    }
}
