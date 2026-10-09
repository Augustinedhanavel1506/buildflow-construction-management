package com.buildflow.dealer.dto;

import com.buildflow.dealer.entity.Dealer;
import com.buildflow.dealer.entity.DealerRate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DealerResponse(
        Long id,
        String name,
        String contactPerson,
        String phone,
        String email,
        String address,
        String district,
        Integer deliveryRadiusKm,
        String gstin,
        String notes,
        boolean active,
        List<Rate> rates
) {
    public record Rate(Long id, String itemName, String unit, BigDecimal rate, Instant updatedAt) {
        static Rate from(DealerRate rate) {
            return new Rate(rate.getId(), rate.getItemName(), rate.getUnit(), rate.getRate(), rate.getUpdatedAt());
        }
    }

    public static DealerResponse from(Dealer dealer) {
        return new DealerResponse(
                dealer.getId(),
                dealer.getName(),
                dealer.getContactPerson(),
                dealer.getPhone(),
                dealer.getEmail(),
                dealer.getAddress(),
                dealer.getDistrict(),
                dealer.getDeliveryRadiusKm(),
                dealer.getGstin(),
                dealer.getNotes(),
                dealer.isActive(),
                dealer.getRates().stream().map(Rate::from).toList()
        );
    }
}
