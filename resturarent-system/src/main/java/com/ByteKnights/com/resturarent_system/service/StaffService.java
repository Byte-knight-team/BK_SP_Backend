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
        // Validate format first before duplicate checks
        StringBuilder validationErrors = new StringBuilder();

        if (!isValidEmail(request.getEmail())) {
            validationErrors.append("Invalid email format. ");
        }

        if (!isValidPhone(request.getPhone())) {
            validationErrors.append("Phone number must be exactly 10 digits. ");
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
        // This is stored until staff changes password
        String tempPassword = generateTempPassword();

        // ----------------- Build new staff user -----------------
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(role)
                .password(passwordEncoder.encode(tempPassword))
                .passwordChanged(false)
                .inviteStatus(InviteStatus.PENDING)
                .temporaryPassword(tempPassword)
                .build();

        // ----------------- Try to send invite email -----------------
        try {
            emailService.sendStaffInviteEmail(user.getEmail(), user.getUsername(), tempPassword);

            // Email send call completed successfully
            user.setEmailSent(true);
            user.setInviteStatus(InviteStatus.SENT);

        } catch (Exception e) {
            // Any email problem should end here:
            // forced fail, wrong SMTP password, auth issue, host issue, port issue, etc.
            user.setEmailSent(false);
            user.setInviteStatus(InviteStatus.FAILED);

            // Keep technical reason only in backend logs
            log.error(
                    "Email sending failed while creating staff. username={}, email={}",
                    user.getUsername(),
                    user.getEmail(),
                    e
            );
        }

        // Save staff even if email failed
        // This allows admin to resend invite later or manually share temp password
        User savedUser = userRepository.save(user);

        // ----------------- Response -----------------
        // Success: all details except temporaryPassword
        // Fail: all details including temporaryPassword
        return CreateStaffResponse.builder()
                .id(savedUser.getId())
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
        // Find already-created staff member
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate a completely new temporary password on each resend
        String tempPassword = generateTempPassword();

        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setTemporaryPassword(tempPassword);
        user.setPasswordChanged(false);
        user.setInviteStatus(InviteStatus.PENDING);

        // Try sending the email again
        try {
            emailService.sendStaffInviteEmail(user.getEmail(), user.getUsername(), tempPassword);

            user.setEmailSent(true);
            user.setInviteStatus(InviteStatus.SENT);

        } catch (Exception e) {
            user.setEmailSent(false);
            user.setInviteStatus(InviteStatus.FAILED);

            // Keep real reason in logs only
            log.error(
                    "Email sending failed while resending invite. userId={}, username={}, email={}",
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    e
            );
        }

        User savedUser = userRepository.save(user);

        // Same response rule:
        // success -> no temp password
        // fail -> include temp password
        return CreateStaffResponse.builder()
                .id(savedUser.getId())
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
}