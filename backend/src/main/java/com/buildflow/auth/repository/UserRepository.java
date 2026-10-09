package com.buildflow.auth.repository;

import com.buildflow.auth.entity.Role;
import com.buildflow.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByBusinessIdAndRoleOrderByFullNameAsc(Long businessId, Role role);

    Optional<User> findByIdAndBusinessId(Long id, Long businessId);
}
