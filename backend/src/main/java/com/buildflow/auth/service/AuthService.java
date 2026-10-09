package com.buildflow.auth.service;

import com.buildflow.auth.dto.AuthResponse;
import com.buildflow.auth.dto.LoginRequest;
import com.buildflow.auth.dto.RefreshRequest;
import com.buildflow.auth.dto.RegisterRequest;
import com.buildflow.auth.entity.Role;
import com.buildflow.auth.entity.User;
import com.buildflow.auth.repository.UserRepository;
import com.buildflow.business.entity.Business;
import com.buildflow.business.repository.BusinessRepository;
import com.buildflow.common.exception.BadRequestException;
import com.buildflow.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                        BusinessRepository businessRepository,
                        PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager,
                        JwtService jwtService) {
        this.userRepository = userRepository;
        this.businessRepository = businessRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("An account with this email already exists.");
        }

        Business business = new Business(request.businessName());
        businessRepository.save(business);

        User user = new User();
        user.setBusiness(business);
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.ADMIN);
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException("Invalid email or password."));

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest request) {
        String token = request.refreshToken();
        if (!jwtService.isRefreshToken(token)) {
            throw new BadRequestException("Invalid refresh token.");
        }

        String email = jwtService.extractEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token."));

        if (!jwtService.isTokenValid(token, user.getEmail())) {
            throw new BadRequestException("Refresh token has expired.");
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        Business business = user.getBusiness();
        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name(), business.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        AuthResponse.UserSummary summary = new AuthResponse.UserSummary(
                user.getId(), user.getFullName(), user.getEmail(), user.getRole().name(),
                business.getId(), business.getName());

        return new AuthResponse(accessToken, refreshToken, summary);
    }
}
