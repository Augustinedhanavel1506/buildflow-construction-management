package com.buildflow.dealer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

// rates == null leaves the dealer's rate card unchanged; a list (even empty) replaces it.
public record DealerRequest(
        @NotBlank(message = "Dealer name is required") String name,
        String contactPerson,
        String phone,
        String email,
        @Size(max = 500, message = "Address is too long") String address,
        String district,
        @PositiveOrZero(message = "Delivery radius cannot be negative") Integer deliveryRadiusKm,
        String gstin,
        @Size(max = 500, message = "Notes are too long") String notes,
        Boolean active,
        List<@Valid DealerRateRequest> rates
) {
}
