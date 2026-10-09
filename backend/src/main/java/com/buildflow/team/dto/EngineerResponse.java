package com.buildflow.team.dto;

import com.buildflow.auth.entity.User;

public record EngineerResponse(Long id, String fullName, String email, String registrationNo, boolean active) {
    public static EngineerResponse from(User user) {
        return new EngineerResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRegistrationNo(), user.isActive());
    }
}
