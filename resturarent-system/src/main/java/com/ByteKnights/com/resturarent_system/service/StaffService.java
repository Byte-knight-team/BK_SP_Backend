package com.byteknights.com.resturarent_system.service;

import com.byteknights.com.resturarent_system.dto.CreateStaffRequest;
import com.byteknights.com.resturarent_system.dto.CreateStaffResponse;
import com.byteknights.com.resturarent_system.entity.Role;
import com.byteknights.com.resturarent_system.entity.User;
import com.byteknights.com.resturarent_system.repository.RoleRepository;
import com.byteknights.com.resturarent_system.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Random;

@Service
public class StaffService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffService(UserRepository userRepository,
                        RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public CreateStaffResponse createStaff(CreateStaffRequest request) {

        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new RuntimeException("Username is required");
        }

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }

        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new RuntimeException("Phone is required");
        }

        if (request.getRoleName() == null || request.getRoleName().trim().isEmpty()) {
            throw new RuntimeException("Role name is required");
        }

        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail().trim())) {
            throw new RuntimeException("Email already exists");
        }

        if (userRepository.existsByPhone(request.getPhone().trim())) {
            throw new RuntimeException("Phone already exists");
        }

        String requestedRole = request.getRoleName().trim().toUpperCase();

        // Get current logged-in user role
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String currentUserRole = authentication.getAuthorities()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No role found"))
                .getAuthority()
                .replace("ROLE_", "");

        // Rule enforcement
        if (currentUserRole.equals("ADMIN")) {

            if (requestedRole.equals("ADMIN") || requestedRole.equals("SUPER_ADMIN")) {
                throw new RuntimeException("ADMIN cannot create ADMIN or SUPER_ADMIN users");
            }
        }

        if (currentUserRole.equals("SUPER_ADMIN")) {
            // SUPER_ADMIN can create all roles → no restriction
        }

        if (requestedRole.equals("CUSTOMER")) {
            throw new RuntimeException("Staff cannot be created with CUSTOMER role");
        }

        Role role = roleRepository.findByName(requestedRole)
                .orElseThrow(() -> new RuntimeException("Role not found: " + requestedRole));

        String temporaryPassword = generateTemporaryPassword();

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim())
                .phone(request.getPhone().trim())
                .password(passwordEncoder.encode(temporaryPassword))
                .passwordChanged(false)
                .role(role)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        return CreateStaffResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .role(savedUser.getRole().getName())
                .active(savedUser.getIsActive())
                .temporaryPassword(temporaryPassword)
                .build();
    }

    private String generateTemporaryPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghijkmnopqrstuvwxyz";
        String digits = "23456789";
        String special = "@#$%";
        String all = upper + lower + digits + special;

        Random random = new Random();

        StringBuilder password = new StringBuilder();
        password.append(upper.charAt(random.nextInt(upper.length())));
        password.append(lower.charAt(random.nextInt(lower.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));

        for (int i = 4; i < 10; i++) {
            password.append(all.charAt(random.nextInt(all.length())));
        }

        return password.toString();
    }
}