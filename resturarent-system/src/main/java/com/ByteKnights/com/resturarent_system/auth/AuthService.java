package com.ByteKnights.com.resturarent_system.auth;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ByteKnights.com.resturarent_system.dto.ChangePasswordRequest;
import com.ByteKnights.com.resturarent_system.dto.LoginResponse;
import com.ByteKnights.com.resturarent_system.dto.StaffLoginRequest;
import com.ByteKnights.com.resturarent_system.entity.AuditEventType;
import com.ByteKnights.com.resturarent_system.entity.AuditModule;
import com.ByteKnights.com.resturarent_system.entity.AuditSeverity;
import com.ByteKnights.com.resturarent_system.entity.AuditStatus;
import com.ByteKnights.com.resturarent_system.entity.AuditTargetType;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.security.JwtService;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    public AuthService(UserRepository userRepository,
            StaffRepository staffRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
    }

    public LoginResponse loginStaff(StaffLoginRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            auditLogService.logAnonymousAction(
                    request.getEmail(),
                    AuditModule.AUTH,
                    AuditEventType.LOGIN_FAILED,
                    AuditStatus.FAILURE,
                    AuditSeverity.WARN,
                    AuditTargetType.AUTH,
                    null,
                    "Staff login failed: invalid email",
                    null,
                    null);
            throw new RuntimeException("Invalid email");
        }

        User user = userOptional.get();
        Staff staff = staffRepository.findByUserId(user.getId()).orElse(null);
        Long branchId = getBranchId(staff);

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            auditLogService.logActionAsUser(
                    user.getId(),
                    user.getEmail(),
                    user.getRole() != null ? user.getRole().getName() : null,
                    branchId,
                    AuditModule.AUTH,
                    AuditEventType.LOGIN_FAILED,
                    AuditStatus.FAILURE,
                    AuditSeverity.WARN,
                    AuditTargetType.AUTH,
                    user.getId(),
                    "Staff login failed: account disabled",
                    null,
                    null);
            throw new RuntimeException("Account disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            auditLogService.logActionAsUser(
                    user.getId(),
                    user.getEmail(),
                    user.getRole() != null ? user.getRole().getName() : null,
                    branchId,
                    AuditModule.AUTH,
                    AuditEventType.LOGIN_FAILED,
                    AuditStatus.FAILURE,
                    AuditSeverity.WARN,
                    AuditTargetType.AUTH,
                    user.getId(),
                    "Staff login failed: invalid password",
                    null,
                    null);
            throw new RuntimeException("Invalid password");
        }

        String roleName = user.getRole() != null ? user.getRole().getName() : null;

        /*
         * Non-super-admin staff must be connected to an active branch.
         * These failures are audited because they directly affect login access.
         */
        if (!"SUPER_ADMIN".equals(roleName)) {

            if (staff == null || staff.getBranch() == null) {
                auditLogService.logActionAsUser(
                        user.getId(),
                        user.getEmail(),
                        user.getRole() != null ? user.getRole().getName() : null,
                        null,
                        AuditModule.AUTH,
                        AuditEventType.LOGIN_FAILED,
                        AuditStatus.FAILURE,
                        AuditSeverity.WARN,
                        AuditTargetType.AUTH,
                        user.getId(),
                        "Staff login failed: branch not assigned",
                        null,
                        null);

                throw new RuntimeException("Staff branch is not assigned");
            }

            if (!"ACTIVE".equals(String.valueOf(staff.getBranch().getStatus()))) {
                auditLogService.logActionAsUser(
                        user.getId(),
                        user.getEmail(),
                        user.getRole() != null ? user.getRole().getName() : null,
                        branchId,
                        AuditModule.AUTH,
                        AuditEventType.LOGIN_FAILED,
                        AuditStatus.FAILURE,
                        AuditSeverity.WARN,
                        AuditTargetType.AUTH,
                        user.getId(),
                        "Staff login failed: branch inactive",
                        null,
                        null);

                throw new RuntimeException("Your branch is inactive. Please contact the system administrator.");
            }
        }

        String branchName = null;

        /*
         * branchId is already declared above using getBranchId(staff).
         * Here we only prepare branchName for the JWT payload.
         */
        if (staff != null && staff.getBranch() != null) {
            branchName = staff.getBranch().getName();
        }

        /*
         * We generate a jwt token for the user using the generateToken method of JwtService.
         * The token is valid for 08 hours.
         */
        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().getName(),
                branchId,
                branchName);

        Map<String, Object> loginDetails = new LinkedHashMap<>();
        loginDetails.put("userId", user.getId());
        loginDetails.put("email", user.getEmail());
        loginDetails.put("roleName", user.getRole() != null ? user.getRole().getName() : null);
        loginDetails.put("branchId", branchId);

        auditLogService.logActionAsUser(
                user.getId(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().getName() : null,
                branchId,
                AuditModule.AUTH,
                AuditEventType.LOGIN_SUCCESS,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.AUTH,
                user.getId(),
                "Staff login successful",
                null,
                loginDetails);

        return LoginResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roleName(user.getRole().getName())
                .active(user.getIsActive())
                .passwordChanged(user.getPasswordChanged())
                .branchId(branchId)
                .branchName(branchName)
                .token(token)
                .tokenType("Bearer")
                .build();
    }

    /*
     * Changes password for the currently authenticated staff user.
     * Manual audit is used so we can track passwordChanged before/after.
     * The actual password value is never stored in audit logs.
     */
    @Transactional
    public String changePassword(ChangePasswordRequest request) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email;

        if (principal instanceof JwtUserPrincipal jwtUser) {
            email = jwtUser.getEmail();
        } else if (principal instanceof User user) {
            email = user.getEmail();
        } else {
            throw new RuntimeException("Authenticated user not found");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUserId(user.getId()).orElse(null);
        Long branchId = getBranchId(staff);

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

        Map<String, Object> oldValues = new LinkedHashMap<>();
        oldValues.put("passwordChanged", user.getPasswordChanged());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChanged(true);
        userRepository.save(user);

        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("passwordChanged", user.getPasswordChanged());

        auditLogService.logCurrentUserAction(
                AuditModule.AUTH,
                AuditEventType.PASSWORD_CHANGED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.USER,
                user.getId(),
                branchId,
                "Staff password changed successfully",
                oldValues,
                newValues);

        return "Password changed successfully";
    }

    private Long getBranchId(Staff staff) {
        if (staff != null && staff.getBranch() != null) {
            return staff.getBranch().getId();
        }
        return null;
    }
}