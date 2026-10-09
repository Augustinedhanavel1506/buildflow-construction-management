package com.buildflow.common.security;

import com.buildflow.auth.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public Long getCurrentBusinessId() {
        return getCurrentUser().getBusiness().getId();
    }
}
