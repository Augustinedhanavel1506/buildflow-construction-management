package com.buildflow.business.service;

import com.buildflow.business.dto.BusinessRequest;
import com.buildflow.business.dto.BusinessResponse;
import com.buildflow.business.entity.Business;
import com.buildflow.business.repository.BusinessRepository;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final CurrentUserProvider currentUserProvider;

    public BusinessService(BusinessRepository businessRepository, CurrentUserProvider currentUserProvider) {
        this.businessRepository = businessRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public BusinessResponse get() {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found."));
        return BusinessResponse.from(business);
    }

    @Transactional
    public BusinessResponse update(BusinessRequest request) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found."));

        business.setName(request.name());
        business.setPhone(request.phone());
        business.setAddress(request.address());
        business.setGstin(request.gstin());
        business.setStateName(request.stateName());
        business.setMaterialRegion(request.materialRegion());
        business.setDefaultGstRate(request.defaultGstRate() != null ? request.defaultGstRate() : new BigDecimal("18.00"));

        return BusinessResponse.from(businessRepository.save(business));
    }
}
