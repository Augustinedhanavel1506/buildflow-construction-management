package com.buildflow.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserSummary user
) {
    public record UserSummary(
            Long id,
            String fullName,
            String email,
            String role,
            Long businessId,
            String businessName
    ) {
    }
}
