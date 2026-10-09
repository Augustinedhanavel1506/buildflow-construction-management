package com.buildflow.business.dto;

import com.buildflow.business.entity.Business;

import java.math.BigDecimal;

public record BusinessResponse(
        Long id,
        String name,
        String phone,
        String address,
        String gstin,
        String stateName,
        String materialRegion,
        BigDecimal defaultGstRate
) {
    public static BusinessResponse from(Business business) {
        return new BusinessResponse(
                business.getId(),
                business.getName(),
                business.getPhone(),
                business.getAddress(),
                business.getGstin(),
                business.getStateName(),
                business.getMaterialRegion(),
                business.getDefaultGstRate()
        );
    }
}
