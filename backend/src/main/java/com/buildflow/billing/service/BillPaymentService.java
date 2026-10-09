package com.buildflow.billing.service;

import com.buildflow.billing.dto.BillPaymentRequest;
import com.buildflow.billing.dto.BillPaymentResponse;
import com.buildflow.billing.entity.BillPayment;
import com.buildflow.billing.entity.RaBill;
import com.buildflow.billing.repository.BillPaymentRepository;
import com.buildflow.common.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BillPaymentService {

    private final BillPaymentRepository billPaymentRepository;
    private final RaBillService raBillService;
    private final CurrentUserProvider currentUserProvider;

    public BillPaymentService(BillPaymentRepository billPaymentRepository,
                               RaBillService raBillService,
                               CurrentUserProvider currentUserProvider) {
        this.billPaymentRepository = billPaymentRepository;
        this.raBillService = raBillService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<BillPaymentResponse> list(Long raBillId) {
        raBillService.findOwnedBillForRead(raBillId);
        return billPaymentRepository.findByRaBillIdOrderByPaymentDateDesc(raBillId).stream()
                .map(BillPaymentResponse::from)
                .toList();
    }

    @Transactional
    public BillPaymentResponse create(Long raBillId, BillPaymentRequest request) {
        RaBill bill = raBillService.findOwnedBillForPayment(raBillId);

        BillPayment payment = new BillPayment();
        payment.setRaBill(bill);
        payment.setAmount(request.amount());
        payment.setPaymentDate(request.paymentDate());
        payment.setMode(request.mode());
        payment.setReferenceNumber(request.referenceNumber());
        payment.setRecordedBy(currentUserProvider.getCurrentUser());
        billPaymentRepository.save(payment);

        BigDecimal totalReceived = billPaymentRepository.sumAmountByRaBillId(bill.getId());
        raBillService.markPaidIfFullyReceived(bill, totalReceived);

        return BillPaymentResponse.from(payment);
    }
}
