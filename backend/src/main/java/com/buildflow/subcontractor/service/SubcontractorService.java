package com.buildflow.subcontractor.service;

import com.buildflow.business.entity.Business;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.subcontractor.dto.SubcontractorRequest;
import com.buildflow.subcontractor.dto.SubcontractorResponse;
import com.buildflow.subcontractor.entity.Subcontractor;
import com.buildflow.subcontractor.repository.SubcontractorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubcontractorService {

    private final SubcontractorRepository subcontractorRepository;
    private final CurrentUserProvider currentUserProvider;

    public SubcontractorService(SubcontractorRepository subcontractorRepository,
                                 CurrentUserProvider currentUserProvider) {
        this.subcontractorRepository = subcontractorRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<SubcontractorResponse> list(boolean includeInactive) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        List<Subcontractor> subcontractors = includeInactive
                ? subcontractorRepository.findByBusinessIdOrderByNameAsc(businessId)
                : subcontractorRepository.findByBusinessIdAndActiveTrueOrderByNameAsc(businessId);
        return subcontractors.stream().map(SubcontractorResponse::from).toList();
    }

    @Transactional
    public SubcontractorResponse create(SubcontractorRequest request) {
        Business business = currentUserProvider.getCurrentUser().getBusiness();

        Subcontractor subcontractor = new Subcontractor();
        subcontractor.setBusiness(business);
        applyRequest(subcontractor, request);

        return SubcontractorResponse.from(subcontractorRepository.save(subcontractor));
    }

    @Transactional
    public SubcontractorResponse update(Long id, SubcontractorRequest request) {
        Subcontractor subcontractor = findOwned(id);
        applyRequest(subcontractor, request);
        return SubcontractorResponse.from(subcontractorRepository.save(subcontractor));
    }

    private void applyRequest(Subcontractor subcontractor, SubcontractorRequest request) {
        subcontractor.setName(request.name());
        subcontractor.setTradeType(request.tradeType());
        subcontractor.setContactPerson(request.contactPerson());
        subcontractor.setPhone(request.phone());
        subcontractor.setEmail(request.email());
        subcontractor.setGstNumber(request.gstNumber());
        subcontractor.setPanNumber(request.panNumber());
        subcontractor.setActive(request.active() == null || request.active());
    }

    private Subcontractor findOwned(Long id) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return subcontractorRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Subcontractor not found."));
    }
}
