package com.ByteKnights.com.resturarent_system.auth;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ByteKnights.com.resturarent_system.dto.ChangePasswordRequest;
import com.ByteKnights.com.resturarent_system.dto.LoginResponse;
import com.ByteKnights.com.resturarent_system.dto.StaffLoginRequest;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.security.JwtService;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse loginStaff(StaffLoginRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            throw new RuntimeException("Invalid email");
        }

        User user = userOptional.get();

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Account disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String roleName = user.getRole().getName();
        String token = jwtService.generateToken(user.getEmail(), roleName);

        return LoginResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(roleName)
                .active(user.getIsActive())
                .passwordChanged(user.getPasswordChanged())
                .token(token)
                .tokenType("Bearer")
                .build();
    }

    public String changePassword(ChangePasswordRequest request) {
        Object principal = SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        if (!(principal instanceof User)) {
            throw new RuntimeException("Authenticated user not found");
        }

        User authenticatedUser = (User) principal;

        User user = userRepository.findByEmail(authenticatedUser.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Account disabled");
        }

        if (request.getCurrentPassword() == null || request.getCurrentPassword().trim().isEmpty()) {
            throw new RuntimeException("Current password is required");
        }

        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            throw new RuntimeException("New password is required");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChanged(true);
        userRepository.save(user);

        return "Password changed successfully";
    }
}
