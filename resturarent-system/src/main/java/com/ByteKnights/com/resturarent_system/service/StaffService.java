package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.CreateStaffRequest;
import com.ByteKnights.com.resturarent_system.dto.CreateStaffResponse;
import com.ByteKnights.com.resturarent_system.entity.InviteStatus;
import com.ByteKnights.com.resturarent_system.entity.Role;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.repository.RoleRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class StaffService {

    private static final Logger log = LoggerFactory.getLogger(StaffService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public CreateStaffResponse createStaff(CreateStaffRequest request) {

        // ----------------- Basic input validation -----------------
        StringBuilder validationErrors = new StringBuilder();

        if (isBlank(request.getFullName())) {
            validationErrors.append("Full name is required. ");
        }

        if (isBlank(request.getUsername())) {
            validationErrors.append("Username is required. ");
        }

        if (!isValidEmail(request.getEmail())) {
            validationErrors.append("Invalid email format. ");
        }

        if (!isValidPhone(request.getPhone())) {
            validationErrors.append("Phone number must be exactly 10 digits. ");
        }

        if (isBlank(request.getRoleName())) {
            validationErrors.append("Role is required. ");
        }

        if (validationErrors.length() > 0) {
            return CreateStaffResponse.builder()
                    .message(validationErrors.toString().trim())
                    .build();
        }

        // ----------------- Duplicate checks -----------------
        StringBuilder conflictMsg = new StringBuilder();

        if (userRepository.existsByEmail(request.getEmail())) {
            conflictMsg.append("Email already exists. ");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            conflictMsg.append("Phone number already exists. ");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            conflictMsg.append("Username already exists. ");
        }

        if (conflictMsg.length() > 0) {
            return CreateStaffResponse.builder()
                    .message(conflictMsg.toString().trim())
                    .build();
        }

        // ----------------- Resolve role -----------------
        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new RuntimeException("Role not found: " + request.getRoleName()));

        // ----------------- Generate temporary password -----------------
        String tempPassword = generateTempPassword();

        // ----------------- Build new staff user -----------------
        User user = User.builder()
                .fullName(request.getFullName().trim())
                .username(request.getUsername().trim())
                .email(request.getEmail().trim())
                .phone(request.getPhone().trim())
                .role(role)
                .password(passwordEncoder.encode(tempPassword))
                .passwordChanged(false)
                .inviteStatus(InviteStatus.PENDING)
                .temporaryPassword(tempPassword)
                .build();

        // ----------------- Try to send invite email -----------------
        try {
            emailService.sendStaffInviteEmail(user.getEmail(), user.getUsername(), tempPassword);
            user.setEmailSent(true);
            user.setInviteStatus(InviteStatus.SENT);

        } catch (Exception e) {
            user.setEmailSent(false);
            user.setInviteStatus(InviteStatus.FAILED);

            log.error(
                    "Email sending failed while creating staff. username={}, email={}",
                    user.getUsername(),
                    user.getEmail(),
                    e
            );
        }

        // Save staff even if email failed
        User savedUser = userRepository.save(user);

        return CreateStaffResponse.builder()
                .id(savedUser.getId())
                .fullName(savedUser.getFullName())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .role(savedUser.getRole().getName())
                .active(savedUser.getIsActive())
                .passwordChanged(savedUser.getPasswordChanged())
                .inviteStatus(savedUser.getInviteStatus())
                .emailSent(savedUser.getEmailSent())
                .temporaryPassword(Boolean.TRUE.equals(savedUser.getEmailSent()) ? null : tempPassword)
                .message(Boolean.TRUE.equals(savedUser.getEmailSent()) ? "Email sent successfully" : "Email failed")
                .build();
    }

    @Transactional
    public CreateStaffResponse resendInvite(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String tempPassword = generateTempPassword();

        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setTemporaryPassword(tempPassword);
        user.setPasswordChanged(false);
        user.setInviteStatus(InviteStatus.PENDING);

        try {
            emailService.sendStaffInviteEmail(user.getEmail(), user.getUsername(), tempPassword);
            user.setEmailSent(true);
            user.setInviteStatus(InviteStatus.SENT);

        } catch (Exception e) {
            user.setEmailSent(false);
            user.setInviteStatus(InviteStatus.FAILED);

            log.error(
                    "Email sending failed while resending invite. userId={}, username={}, email={}",
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    e
            );
        }

        User savedUser = userRepository.save(user);

        return CreateStaffResponse.builder()
                .id(savedUser.getId())
                .fullName(savedUser.getFullName())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .role(savedUser.getRole().getName())
                .active(savedUser.getIsActive())
                .passwordChanged(savedUser.getPasswordChanged())
                .inviteStatus(savedUser.getInviteStatus())
                .emailSent(savedUser.getEmailSent())
                .temporaryPassword(Boolean.TRUE.equals(savedUser.getEmailSent()) ? null : tempPassword)
                .message(Boolean.TRUE.equals(savedUser.getEmailSent()) ? "Email resent successfully" : "Email failed")
                .build();
    }

    // ----------------- Helper: generate temporary password -----------------
    private String generateTempPassword() {
        int length = 10;
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }

        return sb.toString();
    }

    // ----------------- Helper: validate email format -----------------
    private boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    // ----------------- Helper: validate 10-digit phone -----------------
    private boolean isValidPhone(String phone) {
        if (phone == null) {
            return false;
        }
        return phone.matches("^\\d{10}$");
    }

    // ----------------- Helper: check blank strings -----------------
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}