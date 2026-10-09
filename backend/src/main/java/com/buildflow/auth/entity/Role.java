package com.buildflow.auth.entity;

public enum Role {
    ADMIN,
    PROJECT_MANAGER,
    SITE_SUPERVISOR,
    // Reviews and validates estimates; limited to review requests, calculators and notifications.
    ENGINEER
}
