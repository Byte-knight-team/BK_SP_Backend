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

/*
 * AuthService contains the main authentication logic for staff users.
 */
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

    /*
     * Handles staff login.
     */
    public LoginResponse loginStaff(StaffLoginRequest request) {

        /*
         * Find the user by email.
         */
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        /*
         * If no user is found, log the failed login attempt.
         */
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

        /*
         * Extract the user from Optional after confirming it exists.
         */
        User user = userOptional.get();

        /*
         * Find the Staff record connected to this User, SUPER_ADMIN can login without
         * staff
         * record => branchId = null
         */
        Staff staff = staffRepository.findByUserId(user.getId()).orElse(null);

        /*
         * Get branch ID if the staff member is assigned to a branch.
         * This is used for JWT payload and audit logging.
         */
        Long branchId = getBranchId(staff);

        /*
         * Block login if the user account is deactivated.
         */
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

        /*
         * Verify the raw password entered by the user against the hashed password
         * stored in the database.
         */
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

        /*
         * Get the role name from the user's assigned role.
         */
        String roleName = user.getRole() != null ? user.getRole().getName() : null;

        /*
         * Except SUPER_ADMIN, other staff roles must belong to an active branch.
         */
        if (!"SUPER_ADMIN".equals(roleName)) {

            /*
             * Non-super-admin users must have a Staff record and branch.
             */
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

            /*
             * Block login if the assigned branch is inactive.
             */
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

        /*
         * Prepare branch name for the login response and JWT token.
         */
        String branchName = null;

        if (staff != null && staff.getBranch() != null) {
            branchName = staff.getBranch().getName();
        }

        /*
         * Generate JWT token with user, role, and branch details.
         */
        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().getName(),
                branchId,
                branchName);

        /*
         * Store non-sensitive login details for audit logging.
         * Passwords are never stored in audit logs.
         */
        Map<String, Object> loginDetails = new LinkedHashMap<>();
        loginDetails.put("userId", user.getId());
        loginDetails.put("email", user.getEmail());
        loginDetails.put("roleName", user.getRole() != null ? user.getRole().getName() : null);
        loginDetails.put("branchId", branchId);

        /*
         * Save successful login audit log.
         */
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

        /*
         * Return login response to frontend.
         * The tokenType is Bearer because frontend sends, Authorization: Bearer <token>
         */
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
        Changes the logged-in staff user's password..
     */
    @Transactional
    public String changePassword(ChangePasswordRequest request) {

        /*
           After JWT validation, the authenticated user is stored as the principal.
         */
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String email;

        /*
           In this project, JWT authentication usually stores JwtUserPrincipal.
         */
        if (principal instanceof JwtUserPrincipal jwtUser) {
            email = jwtUser.getEmail();
        }

        /*
           This is a fallback in case the principal is a User object.
         */
        else if (principal instanceof User user) {
            email = user.getEmail();
        }

        /*
           If principal type is unknown, the authenticated user cannot be identified.
         */
        else {
            throw new RuntimeException("Authenticated user not found");
        }

        /*
         * Find the full User entity from the database using email.
         */
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        /*
         * Get staff and branch details for audit logging.
         */
        Staff staff = staffRepository.findByUserId(user.getId()).orElse(null);
        Long branchId = getBranchId(staff);

        /*
         * Deactivated users cannot change passwords.
         */
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Account disabled");
        }

        /*
         * Validate current password field.
         */
        if (request.getCurrentPassword() == null || request.getCurrentPassword().trim().isEmpty()) {
            throw new RuntimeException("Current password is required");
        }

        /*
         * Validate new password field.
         */
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            throw new RuntimeException("New password is required");
        }

        /*
         * Confirm the current password before allowing password change.
         */
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        /*
         * Keep old audit values before changing the password
         */
        Map<String, Object> oldValues = new LinkedHashMap<>();
        oldValues.put("passwordChanged", user.getPasswordChanged());

        /*
         * Hash the new password before saving it
         */
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChanged(true);
        userRepository.save(user);

        /*
         * Keep new audit values after password update
         */
        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("passwordChanged", user.getPasswordChanged());

        /*
         * Save audit log for successful password change
         */
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

    /*
     * Helper method to safely get branch ID from Staff.
     * Returns null when the staff record or branch is missing.
     */
    private Long getBranchId(Staff staff) {
        if (staff != null && staff.getBranch() != null) {
            return staff.getBranch().getId();
        }

        return null;
    }
}