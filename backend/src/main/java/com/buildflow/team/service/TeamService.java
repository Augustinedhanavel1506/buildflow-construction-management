package com.buildflow.team.service;

import com.buildflow.auth.entity.Role;
import com.buildflow.auth.entity.User;
import com.buildflow.auth.repository.UserRepository;
import com.buildflow.common.exception.BadRequestException;
import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.team.dto.EngineerRequest;
import com.buildflow.team.dto.EngineerResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeamService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;

    public TeamService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        CurrentUserProvider currentUserProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<EngineerResponse> listEngineers() {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return userRepository.findByBusinessIdAndRoleOrderByFullNameAsc(businessId, Role.ENGINEER).stream()
                .map(EngineerResponse::from)
                .toList();
    }

    @Transactional
    public EngineerResponse createEngineer(EngineerRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with this email already exists.");
        }
        User engineer = new User();
        engineer.setBusiness(currentUserProvider.getCurrentUser().getBusiness());
        engineer.setFullName(request.fullName().trim());
        engineer.setEmail(email);
        engineer.setPasswordHash(passwordEncoder.encode(request.password()));
        engineer.setRole(Role.ENGINEER);
        engineer.setRegistrationNo(request.registrationNo() == null || request.registrationNo().isBlank()
                ? null : request.registrationNo().trim());
        return EngineerResponse.from(userRepository.save(engineer));
    }

    @Transactional
    public EngineerResponse setActive(Long id, boolean active) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        User engineer = userRepository.findByIdAndBusinessId(id, businessId)
                .filter(u -> u.getRole() == Role.ENGINEER)
                .orElseThrow(() -> new ResourceNotFoundException("Engineer not found."));
        engineer.setActive(active);
        return EngineerResponse.from(userRepository.save(engineer));
    }
}
