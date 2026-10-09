package com.buildflow.dealer.dto;

import com.buildflow.dealer.entity.DealerQuoteStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record DealerQuoteUpdateRequest(
        @NotNull(message = "Status is required") DealerQuoteStatus status,
        @PositiveOrZero(message = "Delivery charge cannot be negative") BigDecimal deliveryCharge,
        @PositiveOrZero(message = "Loading charge cannot be negative") BigDecimal loadingCharge,
        @Size(max = 500, message = "Notes are too long") String notes,
        List<@Valid Line> lines
) {
    // unitRate null clears the dealer's price for that item.
    public record Line(
            @NotBlank(message = "Item name is required") String itemName,
            @Positive(message = "Rate must be greater than zero") BigDecimal unitRate
    ) {
    }
}
