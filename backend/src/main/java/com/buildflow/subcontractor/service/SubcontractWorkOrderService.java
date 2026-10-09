package com.buildflow.subcontractor.service;

import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.project.entity.Project;
import com.buildflow.project.repository.ProjectRepository;
import com.buildflow.subcontractor.dto.WorkOrderRequest;
import com.buildflow.subcontractor.dto.WorkOrderResponse;
import com.buildflow.subcontractor.entity.SubcontractWorkOrder;
import com.buildflow.subcontractor.entity.Subcontractor;
import com.buildflow.subcontractor.entity.SubcontractorBill;
import com.buildflow.subcontractor.entity.SubcontractorBillStatus;
import com.buildflow.subcontractor.repository.SubcontractWorkOrderRepository;
import com.buildflow.subcontractor.repository.SubcontractorBillRepository;
import com.buildflow.subcontractor.repository.SubcontractorPaymentRepository;
import com.buildflow.subcontractor.repository.SubcontractorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SubcontractWorkOrderService {

    private final SubcontractWorkOrderRepository workOrderRepository;
    private final SubcontractorRepository subcontractorRepository;
    private final SubcontractorBillRepository subcontractorBillRepository;
    private final SubcontractorPaymentRepository subcontractorPaymentRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserProvider currentUserProvider;

    public SubcontractWorkOrderService(SubcontractWorkOrderRepository workOrderRepository,
                                        SubcontractorRepository subcontractorRepository,
                                        SubcontractorBillRepository subcontractorBillRepository,
                                        SubcontractorPaymentRepository subcontractorPaymentRepository,
                                        ProjectRepository projectRepository,
                                        CurrentUserProvider currentUserProvider) {
        this.workOrderRepository = workOrderRepository;
        this.subcontractorRepository = subcontractorRepository;
        this.subcontractorBillRepository = subcontractorBillRepository;
        this.subcontractorPaymentRepository = subcontractorPaymentRepository;
        this.projectRepository = projectRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<WorkOrderResponse> list(Long projectId) {
        Project project = findOwnedProject(projectId);
        return workOrderRepository.findByProjectIdOrderByCreatedAtDesc(project.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WorkOrderResponse create(Long projectId, WorkOrderRequest request) {
        Project project = findOwnedProject(projectId);
        Long businessId = currentUserProvider.getCurrentBusinessId();

        Subcontractor subcontractor = subcontractorRepository.findByIdAndBusinessId(request.subcontractorId(), businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Subcontractor not found."));

        SubcontractWorkOrder workOrder = new SubcontractWorkOrder();
        workOrder.setProject(project);
        workOrder.setSubcontractor(subcontractor);
        workOrder.setTitle(request.title());
        workOrder.setScopeDescription(request.scopeDescription());
        workOrder.setContractType(request.contractType());
        workOrder.setContractValue(request.contractValue());
        workOrder.setStartDate(request.startDate());
        workOrder.setEndDate(request.endDate());
        workOrder.setCreatedBy(currentUserProvider.getCurrentUser());

        return toResponse(workOrderRepository.save(workOrder));
    }

    private WorkOrderResponse toResponse(SubcontractWorkOrder workOrder) {
        List<SubcontractorBill> bills =
                subcontractorBillRepository.findByWorkOrderIdOrderByBillDateDesc(workOrder.getId());

        BigDecimal totalBilled = bills.stream()
                .filter(bill -> bill.getStatus() != SubcontractorBillStatus.DRAFT)
                .map(SubcontractorBill::getNetPayableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = bills.stream()
                .map(bill -> subcontractorPaymentRepository.sumAmountBySubcontractorBillId(bill.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return WorkOrderResponse.from(workOrder, totalBilled, totalPaid);
    }

    Project findOwnedProject(Long projectId) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return projectRepository.findByIdAndBusinessId(projectId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
    }

    SubcontractWorkOrder findOwnedWorkOrder(Long id) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return workOrderRepository.findByIdAndProjectBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found."));
    }
}
