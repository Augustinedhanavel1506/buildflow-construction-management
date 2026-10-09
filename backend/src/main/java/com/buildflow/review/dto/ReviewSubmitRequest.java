package com.buildflow.review.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

// COMPLETE validates the listed lines (optionally correcting quantities) and, with
// approveRemaining, every other preliminary line as estimated. REQUEST_CHANGES validates nothing
// and sends the estimate back with a note.
public record ReviewSubmitRequest(
        @NotNull(message = "Outcome is required") Outcome outcome,
        @Size(max = 1000, message = "Note is too long") String overallNote,
        Boolean approveRemaining,
        List<@Valid Line> lines
) {
    public enum Outcome {
        COMPLETE,
        REQUEST_CHANGES
    }

    public record Line(
            @NotNull(message = "Line is required") Long boqItemId,
            @Positive(message = "Quantity must be greater than zero") BigDecimal quantity,
            @Size(max = 500, message = "Comment is too long") String comment
    ) {
    }
}
