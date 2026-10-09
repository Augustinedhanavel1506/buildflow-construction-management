package com.buildflow.subcontractor.dto;

import com.buildflow.subcontractor.entity.Subcontractor;

public record SubcontractorResponse(
        Long id,
        String name,
        String tradeType,
        String contactPerson,
        String phone,
        String email,
        String gstNumber,
        String panNumber,
        boolean active
) {
    public static SubcontractorResponse from(Subcontractor subcontractor) {
        return new SubcontractorResponse(
                subcontractor.getId(),
                subcontractor.getName(),
                subcontractor.getTradeType(),
                subcontractor.getContactPerson(),
                subcontractor.getPhone(),
                subcontractor.getEmail(),
                subcontractor.getGstNumber(),
                subcontractor.getPanNumber(),
                subcontractor.isActive()
        );
    }
}
