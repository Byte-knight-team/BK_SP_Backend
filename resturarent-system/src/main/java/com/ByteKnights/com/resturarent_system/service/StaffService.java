package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.CreateStaffRequest;
import com.ByteKnights.com.resturarent_system.dto.CreateStaffResponse;
import com.ByteKnights.com.resturarent_system.entity.InviteStatus;
import com.ByteKnights.com.resturarent_system.entity.Role;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.repository.RoleRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.service.email.EmailService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class StaffService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public StaffService(UserRepository userRepository,
                        RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder,
                        EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String currentUserRole = authentication.getAuthorities()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No role found"))
                .getAuthority()
                .replace("ROLE_", "");

        if (currentUserRole.equals("ADMIN")) {
            if (requestedRole.equals("ADMIN") || requestedRole.equals("SUPER_ADMIN")) {
                throw new RuntimeException("ADMIN cannot create ADMIN or SUPER_ADMIN users");
            }
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
                .inviteStatus(InviteStatus.PENDING)
                .lastInviteAttemptAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        try {
            emailService.sendStaffInviteEmail(
                    savedUser.getEmail(),
                    savedUser.getUsername(),
                    temporaryPassword
            );

            savedUser.setInviteStatus(InviteStatus.SENT);
            savedUser.setLastInviteAttemptAt(LocalDateTime.now());
            userRepository.save(savedUser);

            return CreateStaffResponse.builder()
                    .id(savedUser.getId())
                    .username(savedUser.getUsername())
                    .email(savedUser.getEmail())
                    .phone(savedUser.getPhone())
                    .role(savedUser.getRole().getName())
                    .active(savedUser.getIsActive())
                    .passwordChanged(savedUser.getPasswordChanged())
                    .inviteStatus(savedUser.getInviteStatus())
                    .emailSent(true)
                    .temporaryPassword(null)
                    .message("Staff created successfully and invitation email sent.")
                    .build();

        } catch (Exception e) {
            savedUser.setInviteStatus(InviteStatus.FAILED);
            savedUser.setLastInviteAttemptAt(LocalDateTime.now());
            userRepository.save(savedUser);

            return CreateStaffResponse.builder()
                    .id(savedUser.getId())
                    .username(savedUser.getUsername())
                    .email(savedUser.getEmail())
                    .phone(savedUser.getPhone())
                    .role(savedUser.getRole().getName())
                    .active(savedUser.getIsActive())
                    .passwordChanged(savedUser.getPasswordChanged())
                    .inviteStatus(savedUser.getInviteStatus())
                    .emailSent(false)
                    .temporaryPassword(temporaryPassword)
                    .message("Staff created successfully, but invitation email could not be sent.")
                    .build();
        }
    }

    public CreateStaffResponse resendInvite(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Staff user not found"));

        String newTemporaryPassword = generateTemporaryPassword();

        user.setPassword(passwordEncoder.encode(newTemporaryPassword));
        user.setPasswordChanged(false);
        user.setLastInviteAttemptAt(LocalDateTime.now());

        userRepository.save(user);

        try {
            emailService.sendStaffInviteEmail(
                    user.getEmail(),
                    user.getUsername(),
                    newTemporaryPassword
            );

            user.setInviteStatus(InviteStatus.SENT);
            user.setLastInviteAttemptAt(LocalDateTime.now());
            userRepository.save(user);

            return CreateStaffResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .role(user.getRole().getName())
                    .active(user.getIsActive())
                    .passwordChanged(user.getPasswordChanged())
                    .inviteStatus(user.getInviteStatus())
                    .emailSent(true)
                    .temporaryPassword(null)
                    .message("Invitation email resent successfully.")
                    .build();

        } catch (Exception e) {
            user.setInviteStatus(InviteStatus.FAILED);
            user.setLastInviteAttemptAt(LocalDateTime.now());
            userRepository.save(user);

            return CreateStaffResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .role(user.getRole().getName())
                    .active(user.getIsActive())
                    .passwordChanged(user.getPasswordChanged())
                    .inviteStatus(user.getInviteStatus())
                    .emailSent(false)
                    .temporaryPassword(newTemporaryPassword)
                    .message("Invitation email resend failed.")
                    .build();
        }
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
