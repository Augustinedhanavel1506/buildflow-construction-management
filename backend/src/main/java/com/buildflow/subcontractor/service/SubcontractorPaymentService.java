package com.buildflow.subcontractor.service;

import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.expense.dto.ExpenseRequest;
import com.buildflow.expense.entity.ExpenseCategory;
import com.buildflow.expense.service.ExpenseService;
import com.buildflow.subcontractor.dto.SubcontractorPaymentRequest;
import com.buildflow.subcontractor.dto.SubcontractorPaymentResponse;
import com.buildflow.subcontractor.entity.SubcontractWorkOrder;
import com.buildflow.subcontractor.entity.SubcontractorBill;
import com.buildflow.subcontractor.entity.SubcontractorPayment;
import com.buildflow.subcontractor.repository.SubcontractorPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SubcontractorPaymentService {

    private final SubcontractorPaymentRepository subcontractorPaymentRepository;
    private final SubcontractorBillService subcontractorBillService;
    private final ExpenseService expenseService;
    private final CurrentUserProvider currentUserProvider;

    public SubcontractorPaymentService(SubcontractorPaymentRepository subcontractorPaymentRepository,
                                        SubcontractorBillService subcontractorBillService,
                                        ExpenseService expenseService,
                                        CurrentUserProvider currentUserProvider) {
        this.subcontractorPaymentRepository = subcontractorPaymentRepository;
        this.subcontractorBillService = subcontractorBillService;
        this.expenseService = expenseService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<SubcontractorPaymentResponse> list(Long subcontractorBillId) {
        subcontractorBillService.findOwnedBillForRead(subcontractorBillId);
        return subcontractorPaymentRepository.findBySubcontractorBillIdOrderByPaymentDateDesc(subcontractorBillId).stream()
                .map(SubcontractorPaymentResponse::from)
                .toList();
    }

    @Transactional
    public SubcontractorPaymentResponse create(Long subcontractorBillId, SubcontractorPaymentRequest request) {
        SubcontractorBill bill = subcontractorBillService.findOwnedBillForPayment(subcontractorBillId);
        SubcontractWorkOrder workOrder = bill.getWorkOrder();

        SubcontractorPayment payment = new SubcontractorPayment();
        payment.setSubcontractorBill(bill);
        payment.setAmount(request.amount());
        payment.setPaymentDate(request.paymentDate());
        payment.setMode(request.mode());
        payment.setReferenceNumber(request.referenceNumber());
        payment.setRecordedBy(currentUserProvider.getCurrentUser());
        subcontractorPaymentRepository.save(payment);

        BigDecimal totalPaid = subcontractorPaymentRepository.sumAmountBySubcontractorBillId(bill.getId());
        subcontractorBillService.markPaidIfFullyReceived(bill, totalPaid);

        // A subcontractor payment is a real project cost — record it as an expense so it
        // flows into actual cost, budget-variance alerts, and reporting like any other spend.
        expenseService.create(workOrder.getProject().getId(), new ExpenseRequest(
                ExpenseCategory.SUBCONTRACTOR,
                request.amount(),
                request.paymentDate(),
                workOrder.getSubcontractor().getName(),
                "Payment for %s (Bill %s)".formatted(workOrder.getTitle(), bill.getBillNumber())
        ));

        return SubcontractorPaymentResponse.from(payment);
    }
}
