package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.CreateStaffRequest;
import com.ByteKnights.com.resturarent_system.dto.CreateStaffResponse;
import com.ByteKnights.com.resturarent_system.dto.StaffResponse;
import com.ByteKnights.com.resturarent_system.dto.UpdateStaffRequest;
import com.ByteKnights.com.resturarent_system.entity.AuditEventType;
import com.ByteKnights.com.resturarent_system.entity.AuditModule;
import com.ByteKnights.com.resturarent_system.entity.AuditSeverity;
import com.ByteKnights.com.resturarent_system.entity.AuditStatus;
import com.ByteKnights.com.resturarent_system.entity.AuditTargetType;
import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.entity.BranchStatus;
import com.ByteKnights.com.resturarent_system.entity.EmploymentStatus;
import com.ByteKnights.com.resturarent_system.entity.InviteStatus;
import com.ByteKnights.com.resturarent_system.entity.Role;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import com.ByteKnights.com.resturarent_system.repository.RoleRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import com.ByteKnights.com.resturarent_system.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class StaffService {

    private static final Logger log = LoggerFactory.getLogger(StaffService.class);

    /*
     * Roles are now database-driven.
     * 
     * Any role in the roles table can be used as a staff role,
     * except roles listed in NON_STAFF_ROLES.
     * 
     * Example:
     * If we create LINE_CHEF in the roles table, the staff creation flow can use it
     * without adding LINE_CHEF here manually.
     */
    private static final Set<String> NON_STAFF_ROLES = Set.of(
            "CUSTOMER");

    /*
     * ADMIN should not be able to create high-level/global roles.
     * 
     * ADMIN can create branch-level operational staff roles such as:
     * MANAGER, CHEF, LINE_CHEF, RECEPTIONIST, DELIVERY, CASHIER, WAITER, etc.
     */
    private static final Set<String> ADMIN_BLOCKED_CREATION_ROLES = Set.of(
            "CUSTOMER",
            "SUPER_ADMIN",
            "ADMIN");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    @Transactional
    public CreateStaffResponse createStaff(CreateStaffRequest request) {

        String normalizedRoleName = normalize(request.getRoleName());

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

        if (isBlank(normalizedRoleName)) {
            validationErrors.append("Role is required. ");
        } else if (!isStaffRoleName(normalizedRoleName)) {
            validationErrors.append("Only staff roles can be used in staff creation. ");
        }

        if (!"SUPER_ADMIN".equals(normalizedRoleName) && request.getBranchId() == null) {
            validationErrors.append("Branch is required for non-super-admin staff. ");
        }

        /*
         * Salary is optional during staff creation.
         * 
         * If provided, it must not be negative.
         * If not provided, backend will use the role default salary.
         */
        if (request.getSalary() != null && request.getSalary().compareTo(BigDecimal.ZERO) < 0) {
            validationErrors.append("Salary cannot be negative. ");
        }

        if (validationErrors.length() > 0) {
            throw new RuntimeException(validationErrors.toString().trim());
        }

        String email = request.getEmail().trim();
        String phone = request.getPhone().trim();
        String username = request.getUsername().trim();
        String fullName = request.getFullName().trim();

        StringBuilder conflictMsg = new StringBuilder();

        if (userRepository.existsByEmail(email)) {
            conflictMsg.append("Email already exists. ");
        }

        if (userRepository.existsByPhone(phone)) {
            conflictMsg.append("Phone number already exists. ");
        }

        if (userRepository.existsByUsername(username)) {
            conflictMsg.append("Username already exists. ");
        }

        if (conflictMsg.length() > 0) {
            throw new RuntimeException(conflictMsg.toString().trim());
        }

        Role role = roleRepository.findByName(normalizedRoleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + normalizedRoleName));

        /*
         * Salary selection rule:
         * 1. If request.salary is provided, use it.
         * 2. Otherwise, copy role.baseSalary.
         */
        BigDecimal resolvedSalary = resolveStaffSalary(request.getSalary(), role);

        User creator = getCurrentAuthenticatedUser();
        String creatorRole = normalize(creator.getRole().getName());

        Branch targetBranch = resolveTargetBranch(creator, creatorRole, normalizedRoleName, request.getBranchId());

        String tempPassword = generateTempPassword();

        User user = User.builder()
                .fullName(fullName)
                .username(username)
                .email(email)
                .phone(phone)
                .role(role)
                .password(passwordEncoder.encode(tempPassword))
                .passwordChanged(false)
                .inviteStatus(InviteStatus.PENDING)
                .temporaryPassword(null)
                .emailSent(false)
                .build();

        User savedUser = userRepository.save(user);

        if (!"SUPER_ADMIN".equals(normalizedRoleName)) {
            Staff staff = Staff.builder()
                    .user(savedUser)
                    .branch(targetBranch)

                    /*
                     * Store actual salary for this individual staff member.
                     * This starts from role default salary unless request.salary overrides it.
                     */
                    .salary(resolvedSalary)

                    .employmentStatus(EmploymentStatus.ACTIVE)
                    .build();

            staffRepository.save(staff);
        }

        try {
            emailService.sendStaffInviteEmail(savedUser.getEmail(), savedUser.getUsername(), tempPassword);
            savedUser.setEmailSent(true);
            savedUser.setInviteStatus(InviteStatus.SENT);

        } catch (Exception e) {
            savedUser.setEmailSent(false);
            savedUser.setInviteStatus(InviteStatus.FAILED);

            log.error(
                    "Email sending failed while creating staff. username={}, email={}",
                    savedUser.getUsername(),
                    savedUser.getEmail(),
                    e);
        }

        User finalSavedUser = userRepository.save(savedUser);
        Staff finalStaff = staffRepository.findByUserId(finalSavedUser.getId()).orElse(null);

        auditLogService.logCurrentUserAction(
                AuditModule.STAFF,
                AuditEventType.STAFF_CREATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.USER,
                finalSavedUser.getId(),
                getBranchId(finalStaff),
                "Staff created successfully",
                null,
                buildStaffAuditSnapshot(finalSavedUser, finalStaff));

        return CreateStaffResponse.builder()
                .id(finalSavedUser.getId())
                .fullName(finalSavedUser.getFullName())
                .username(finalSavedUser.getUsername())
                .email(finalSavedUser.getEmail())
                .phone(finalSavedUser.getPhone())
                .roleName(finalSavedUser.getRole().getName())
                .active(finalSavedUser.getIsActive())
                .passwordChanged(finalSavedUser.getPasswordChanged())
                .inviteStatus(finalSavedUser.getInviteStatus())
                .emailSent(finalSavedUser.getEmailSent())
                .temporaryPassword(Boolean.TRUE.equals(finalSavedUser.getEmailSent()) ? null : tempPassword)
                .branchId(targetBranch != null ? targetBranch.getId() : null)
                .branchName(targetBranch != null ? targetBranch.getName() : null)
                .message(
                        Boolean.TRUE.equals(finalSavedUser.getEmailSent()) ? "Email sent successfully" : "Email failed")
                .build();
    }

    @Transactional
    public CreateStaffResponse resendInvite(Long userId) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User creator = getCurrentAuthenticatedUser();
        String creatorRole = normalize(creator.getRole().getName());
        String targetRole = normalize(targetUser.getRole().getName());

        Staff targetStaff = staffRepository.findByUserId(userId).orElse(null);
        Map<String, Object> oldValues = buildStaffAuditSnapshot(targetUser, targetStaff);

        if ("SUPER_ADMIN".equals(creatorRole)) {
            // SUPER_ADMIN can resend for anyone in staff flow
            // including SUPER_ADMIN or branch staff
        } else if ("ADMIN".equals(creatorRole)) {
            if (targetStaff == null) {
                throw new RuntimeException("ADMIN cannot resend invite for this user");
            }

            Staff creatorStaff = staffRepository.findByUserId(creator.getId())
                    .orElseThrow(() -> new RuntimeException("Admin staff profile not found"));

            Long adminBranchId = creatorStaff.getBranch().getId();
            Long targetBranchId = targetStaff.getBranch().getId();

            if (!adminBranchId.equals(targetBranchId)) {
                throw new RuntimeException("ADMIN can resend invite only for staff in their own branch");
            }

            if (!isAdminCreatableRoleName(targetRole)) {
                throw new RuntimeException("ADMIN can resend invite only for MANAGER, CHEF, RECEPTIONIST, or DELIVERY");
            }
        } else {
            throw new RuntimeException("Only SUPER_ADMIN or ADMIN can resend invites");
        }

        String tempPassword = generateTempPassword();

        targetUser.setPassword(passwordEncoder.encode(tempPassword));
        targetUser.setTemporaryPassword(null);
        targetUser.setPasswordChanged(false);
        targetUser.setInviteStatus(InviteStatus.PENDING);

        try {
            emailService.sendStaffInviteEmail(targetUser.getEmail(), targetUser.getUsername(), tempPassword);
            targetUser.setEmailSent(true);
            targetUser.setInviteStatus(InviteStatus.SENT);

        } catch (Exception e) {
            targetUser.setEmailSent(false);
            targetUser.setInviteStatus(InviteStatus.FAILED);

            log.error(
                    "Email sending failed while resending invite. userId={}, username={}, email={}",
                    targetUser.getId(),
                    targetUser.getUsername(),
                    targetUser.getEmail(),
                    e);
        }

        User savedUser = userRepository.save(targetUser);
        Staff savedStaff = staffRepository.findByUserId(savedUser.getId()).orElse(null);

        auditLogService.logCurrentUserAction(
                AuditModule.STAFF,
                AuditEventType.INVITE_RESENT,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.USER,
                savedUser.getId(),
                getBranchId(savedStaff),
                "Staff invite resent successfully",
                oldValues,
                buildStaffAuditSnapshot(savedUser, savedStaff));

        return CreateStaffResponse.builder()
                .id(savedUser.getId())
                .fullName(savedUser.getFullName())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .roleName(savedUser.getRole().getName())
                .active(savedUser.getIsActive())
                .passwordChanged(savedUser.getPasswordChanged())
                .inviteStatus(savedUser.getInviteStatus())
                .emailSent(savedUser.getEmailSent())
                .temporaryPassword(Boolean.TRUE.equals(savedUser.getEmailSent()) ? null : tempPassword)
                .branchId(savedStaff != null && savedStaff.getBranch() != null ? savedStaff.getBranch().getId() : null)
                .branchName(
                        savedStaff != null && savedStaff.getBranch() != null ? savedStaff.getBranch().getName() : null)
                .message(Boolean.TRUE.equals(savedUser.getEmailSent()) ? "Email resent successfully" : "Email failed")
                .build();
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> getAllStaff() {
        User creator = getCurrentAuthenticatedUser();
        String creatorRole = normalize(creator.getRole().getName());

        if ("SUPER_ADMIN".equals(creatorRole)) {
            return userRepository.findAll().stream()
                    .filter(this::isStaffUser)
                    .map(this::mapToStaffResponse)
                    .sorted(Comparator.comparing(StaffResponse::getId))
                    .toList();
        }

        if ("ADMIN".equals(creatorRole)) {
            Staff creatorStaff = staffRepository.findByUserId(creator.getId())
                    .orElseThrow(() -> new RuntimeException("Admin staff profile not found"));

            Long adminBranchId = creatorStaff.getBranch().getId();

            return userRepository.findAll().stream()
                    .filter(this::isStaffUser)
                    .filter(user -> belongsToBranch(user, adminBranchId))
                    .map(this::mapToStaffResponse)
                    .sorted(Comparator.comparing(StaffResponse::getId))
                    .toList();
        }

        throw new RuntimeException("Only SUPER_ADMIN or ADMIN can view staff");
    }

    @Transactional(readOnly = true)
    public StaffResponse getStaffById(Long userId) {
        User creator = getCurrentAuthenticatedUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Staff user not found"));

        if (!isStaffUser(targetUser)) {
            throw new RuntimeException("This user is not a staff user");
        }

        ensureCanViewTarget(creator, targetUser);

        return mapToStaffResponse(targetUser);
    }

    @Transactional
    public StaffResponse updateStaff(Long userId, UpdateStaffRequest request) {
        User creator = getCurrentAuthenticatedUser();
        String creatorRole = normalize(creator.getRole().getName());

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Staff user not found"));

        if (!isStaffUser(targetUser)) {
            throw new RuntimeException("This user is not a staff user");
        }

        ensureCanManageTarget(creator, targetUser);

        String currentTargetRole = normalize(targetUser.getRole().getName());
        /*
         * These values help us decide whether to update salary from the new role
         * default.
         */
        boolean roleChanged = false;
        Role selectedRoleForSalary = targetUser.getRole();
        Staff targetStaff = staffRepository.findByUserId(targetUser.getId()).orElse(null);
        Map<String, Object> oldValues = buildStaffAuditSnapshot(targetUser, targetStaff);

        if (request.getFullName() != null) {
            String fullName = request.getFullName().trim();
            if (fullName.isEmpty()) {
                throw new RuntimeException("Full name cannot be empty");
            }
            targetUser.setFullName(fullName);
        }

        if (request.getEmail() != null) {
            String email = request.getEmail().trim();
            if (email.isEmpty()) {
                throw new RuntimeException("Email cannot be empty");
            }
            if (!isValidEmail(email)) {
                throw new RuntimeException("Invalid email format");
            }
            if (!email.equalsIgnoreCase(targetUser.getEmail()) && userRepository.existsByEmail(email)) {
                throw new RuntimeException("Email already exists");
            }
            targetUser.setEmail(email);
        }

        if (request.getPhone() != null) {
            String phone = request.getPhone().trim();
            if (phone.isEmpty()) {
                throw new RuntimeException("Phone cannot be empty");
            }
            if (!isValidPhone(phone)) {
                throw new RuntimeException("Phone number must be exactly 10 digits");
            }
            if (!phone.equals(targetUser.getPhone()) && userRepository.existsByPhone(phone)) {
                throw new RuntimeException("Phone number already exists");
            }
            targetUser.setPhone(phone);
        }

        if (request.getRoleName() != null) {
            String newRoleName = normalize(request.getRoleName());
        
            /*
             * Validate the new role using database-driven staff-role rules.
             *
             * CUSTOMER is blocked.
             * Other roles from the roles table can be staff roles.
             */
            if (!isStaffRoleName(newRoleName)) {
                throw new RuntimeException("Only staff roles can be assigned here");
            }
        
            if (!newRoleName.equals(currentTargetRole)) {
                if ("SUPER_ADMIN".equals(currentTargetRole) || "SUPER_ADMIN".equals(newRoleName)) {
                    throw new RuntimeException("Changing to or from SUPER_ADMIN through update is not supported yet");
                }
        
                /*
                 * ADMIN can assign only branch-level staff roles.
                 *
                 * This blocks:
                 * CUSTOMER
                 * SUPER_ADMIN
                 * ADMIN
                 *
                 * This allows:
                 * MANAGER
                 * CHEF
                 * LINE_CHEF
                 * RECEPTIONIST
                 * DELIVERY
                 * future branch-level roles
                 */
                if ("ADMIN".equals(creatorRole) && !isAdminCreatableRoleName(newRoleName)) {
                    throw new RuntimeException("ADMIN can assign only branch-level staff roles");
                }
        
                Role newRole = roleRepository.findByName(newRoleName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + newRoleName));
        
                targetUser.setRole(newRole);
        
                /*
                 * Mark that role changed.
                 *
                 * If request.salary is not provided,
                 * we will copy the new role's base salary to this staff member.
                 */
                roleChanged = true;
                selectedRoleForSalary = newRole;
            }
        }

        /*
         * Update individual staff salary.
         * 
         * Rules:
         * 1. If request.salary is provided, use that value.
         * 2. If role changed and request.salary is not provided,
         * copy the new role base salary.
         * 3. If neither happened, keep existing salary unchanged.
         */
        if (targetStaff != null) {
            if (request.getSalary() != null) {
                targetStaff.setSalary(normalizeSalary(request.getSalary()));
            } else if (roleChanged) {
                targetStaff.setSalary(getBaseSalaryOrZero(selectedRoleForSalary));
            }
        }

        if (targetStaff != null && request.getBranchId() != null) {
            if ("SUPER_ADMIN".equals(creatorRole)) {
                Branch newBranch = branchRepository.findByIdAndStatus(request.getBranchId(), BranchStatus.ACTIVE)
                        .orElseThrow(() -> new RuntimeException("Active branch not found"));
                targetStaff.setBranch(newBranch);
            } else if ("ADMIN".equals(creatorRole)) {
                Staff creatorStaff = staffRepository.findByUserId(creator.getId())
                        .orElseThrow(() -> new RuntimeException("Admin staff profile not found"));

                Long adminBranchId = creatorStaff.getBranch().getId();

                if (!request.getBranchId().equals(adminBranchId)) {
                    throw new RuntimeException("ADMIN can assign staff only to their own branch");
                }

                targetStaff.setBranch(creatorStaff.getBranch());
            }
        }

        User savedUser = userRepository.save(targetUser);

        if (targetStaff != null) {
            staffRepository.save(targetStaff);
        }

        Staff savedStaff = staffRepository.findByUserId(savedUser.getId()).orElse(null);

        auditLogService.logCurrentUserAction(
                AuditModule.STAFF,
                AuditEventType.STAFF_UPDATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.USER,
                savedUser.getId(),
                getBranchId(savedStaff),
                "Staff updated successfully",
                oldValues,
                buildStaffAuditSnapshot(savedUser, savedStaff));

        return mapToStaffResponse(savedUser);
    }

    @Transactional
    public StaffResponse activateStaff(Long userId) {
        User creator = getCurrentAuthenticatedUser();

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Staff user not found"));

        if (!isStaffUser(targetUser)) {
            throw new RuntimeException("This user is not a staff user");
        }

        ensureCanManageTarget(creator, targetUser);

        Staff targetStaff = staffRepository.findByUserId(targetUser.getId()).orElse(null);
        Map<String, Object> oldValues = buildStaffAuditSnapshot(targetUser, targetStaff);

        targetUser.setIsActive(true);
        User savedUser = userRepository.save(targetUser);
        Staff savedStaff = staffRepository.findByUserId(savedUser.getId()).orElse(null);

        auditLogService.logCurrentUserAction(
                AuditModule.STAFF,
                AuditEventType.STAFF_ACTIVATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.USER,
                savedUser.getId(),
                getBranchId(savedStaff),
                "Staff activated successfully",
                oldValues,
                buildStaffAuditSnapshot(savedUser, savedStaff));

        return mapToStaffResponse(savedUser);
    }

    @Transactional
    public StaffResponse deactivateStaff(Long userId) {
        User creator = getCurrentAuthenticatedUser();

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Staff user not found"));

        if (!isStaffUser(targetUser)) {
            throw new RuntimeException("This user is not a staff user");
        }

        ensureCanManageTarget(creator, targetUser);

        Staff targetStaff = staffRepository.findByUserId(targetUser.getId()).orElse(null);
        Map<String, Object> oldValues = buildStaffAuditSnapshot(targetUser, targetStaff);

        targetUser.setIsActive(false);
        User savedUser = userRepository.save(targetUser);
        Staff savedStaff = staffRepository.findByUserId(savedUser.getId()).orElse(null);

        auditLogService.logCurrentUserAction(
                AuditModule.STAFF,
                AuditEventType.STAFF_DEACTIVATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.USER,
                savedUser.getId(),
                getBranchId(savedStaff),
                "Staff deactivated successfully",
                oldValues,
                buildStaffAuditSnapshot(savedUser, savedStaff));

        return mapToStaffResponse(savedUser);
    }

    private Branch resolveTargetBranch(User creator, String creatorRole, String targetRole, Long requestedBranchId) {

        if ("SUPER_ADMIN".equals(creatorRole)) {
            if ("SUPER_ADMIN".equals(targetRole)) {
                return null;
            }

            return branchRepository.findByIdAndStatus(requestedBranchId, BranchStatus.ACTIVE)
                    .orElseThrow(() -> new RuntimeException("Active branch not found"));
        }

        if ("ADMIN".equals(creatorRole)) {
            if (!isAdminCreatableRoleName(targetRole)) {
                throw new RuntimeException("ADMIN can create only MANAGER, CHEF, RECEPTIONIST, or DELIVERY");
            }

            Staff creatorStaff = staffRepository.findByUserId(creator.getId())
                    .orElseThrow(() -> new RuntimeException("Admin staff profile not found"));

            Branch adminBranch = creatorStaff.getBranch();

            if (requestedBranchId != null && !requestedBranchId.equals(adminBranch.getId())) {
                throw new RuntimeException("ADMIN can create staff only for their own branch");
            }

            return adminBranch;
        }

        throw new RuntimeException("Only SUPER_ADMIN or ADMIN can create staff");
    }

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new RuntimeException("Authenticated user not found");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof JwtUserPrincipal jwtUser) {
            return userRepository.findByEmail(jwtUser.getEmail())
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        }

        if (principal instanceof User user) {
            return userRepository.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        }

        String email = authentication.getName();
        if (email != null && !email.trim().isEmpty()) {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        }

        throw new RuntimeException("Authenticated user not found");
    }

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

    private boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private boolean isValidPhone(String phone) {
        if (phone == null) {
            return false;
        }
        return phone.matches("^\\d{10}$");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    /*
     * Checks whether a user belongs to the staff-side system.
     * 
     * This is now database-driven:
     * - CUSTOMER is excluded.
     * - Any other role is treated as a staff-side role.
     */
    private boolean isStaffUser(User user) {
        return user.getRole() != null
                && isStaffRoleName(user.getRole().getName());
    }

    /*
     * Checks whether a role name is allowed to be used as a staff role.
     * 
     * Rule:
     * - CUSTOMER is not a staff role.
     * - Any other role existing in the roles table can be treated as staff-side
     * role.
     */
    private boolean isStaffRoleName(String roleName) {
        String normalizedRoleName = normalize(roleName);

        return normalizedRoleName != null
                && !NON_STAFF_ROLES.contains(normalizedRoleName);
    }

    /*
     * Checks whether ADMIN can create/manage this role.
     * 
     * Rule:
     * - ADMIN cannot create CUSTOMER.
     * - ADMIN cannot create SUPER_ADMIN.
     * - ADMIN cannot create another ADMIN.
     * - ADMIN can create lower branch-level roles, including future roles like
     * LINE_CHEF.
     */
    private boolean isAdminCreatableRoleName(String roleName) {
        String normalizedRoleName = normalize(roleName);

        return normalizedRoleName != null
                && !ADMIN_BLOCKED_CREATION_ROLES.contains(normalizedRoleName);
    }

    private boolean belongsToBranch(User user, Long branchId) {
        return staffRepository.findByUserId(user.getId())
                .map(staff -> staff.getBranch() != null && staff.getBranch().getId().equals(branchId))
                .orElse(false);
    }

    private void ensureCanViewTarget(User creator, User targetUser) {
        String creatorRole = normalize(creator.getRole().getName());

        if ("SUPER_ADMIN".equals(creatorRole)) {
            return;
        }

        if ("ADMIN".equals(creatorRole)) {
            Staff creatorStaff = staffRepository.findByUserId(creator.getId())
                    .orElseThrow(() -> new RuntimeException("Admin staff profile not found"));

            Staff targetStaff = staffRepository.findByUserId(targetUser.getId())
                    .orElseThrow(
                            () -> new RuntimeException("ADMIN can view only branch-linked staff in their own branch"));

            Long adminBranchId = creatorStaff.getBranch().getId();
            Long targetBranchId = targetStaff.getBranch().getId();

            if (!adminBranchId.equals(targetBranchId)) {
                throw new RuntimeException("ADMIN can view staff only in their own branch");
            }

            return;
        }

        throw new RuntimeException("Only SUPER_ADMIN or ADMIN can view staff");
    }

    private void ensureCanManageTarget(User creator, User targetUser) {
        String creatorRole = normalize(creator.getRole().getName());
        String targetRole = normalize(targetUser.getRole().getName());

        if ("SUPER_ADMIN".equals(creatorRole)) {
            return;
        }

        if ("ADMIN".equals(creatorRole)) {
            if (!isAdminCreatableRoleName(targetRole)) {
                throw new RuntimeException("ADMIN can manage only MANAGER, CHEF, RECEPTIONIST, or DELIVERY");
            }

            Staff creatorStaff = staffRepository.findByUserId(creator.getId())
                    .orElseThrow(() -> new RuntimeException("Admin staff profile not found"));

            Staff targetStaff = staffRepository.findByUserId(targetUser.getId())
                    .orElseThrow(() -> new RuntimeException("ADMIN can manage only branch-linked staff"));

            Long adminBranchId = creatorStaff.getBranch().getId();
            Long targetBranchId = targetStaff.getBranch().getId();

            if (!adminBranchId.equals(targetBranchId)) {
                throw new RuntimeException("ADMIN can manage staff only in their own branch");
            }

            return;
        }

        throw new RuntimeException("Only SUPER_ADMIN or ADMIN can manage staff");
    }

    private StaffResponse mapToStaffResponse(User user) {
        Staff staff = staffRepository.findByUserId(user.getId()).orElse(null);

        return StaffResponse.builder()
                .id(user.getId())
                .userId(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .roleName(user.getRole() != null ? user.getRole().getName() : null)
                .active(user.getIsActive())
                .passwordChanged(user.getPasswordChanged())
                .inviteStatus(user.getInviteStatus())
                .emailSent(user.getEmailSent())
                .branchId(staff != null && staff.getBranch() != null ? staff.getBranch().getId() : null)
                .branchName(staff != null && staff.getBranch() != null ? staff.getBranch().getName() : null)
                .employmentStatus(staff != null && staff.getEmploymentStatus() != null
                        ? staff.getEmploymentStatus().name()
                        : null)
                .salary(staff != null ? staff.getSalary() : null) /* Return actual staff salary to frontend. */
                .build();
    }

    private Map<String, Object> buildStaffAuditSnapshot(User user, Staff staff) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        snapshot.put("userId", user.getId());
        snapshot.put("fullName", user.getFullName());
        snapshot.put("username", user.getUsername());
        snapshot.put("email", user.getEmail());
        snapshot.put("phone", user.getPhone());
        snapshot.put("roleName", user.getRole() != null ? user.getRole().getName() : null);
        snapshot.put("active", user.getIsActive());
        snapshot.put("passwordChanged", user.getPasswordChanged());
        snapshot.put("inviteStatus", user.getInviteStatus() != null ? user.getInviteStatus().name() : null);
        snapshot.put("emailSent", user.getEmailSent());
        snapshot.put("branchId", staff != null && staff.getBranch() != null ? staff.getBranch().getId() : null);
        snapshot.put("branchName", staff != null && staff.getBranch() != null ? staff.getBranch().getName() : null);
        snapshot.put("employmentStatus", staff != null && staff.getEmploymentStatus() != null
                ? staff.getEmploymentStatus().name()
                : null);
        snapshot.put("salary", staff != null ? staff.getSalary() : null); /*
                                                                           * Store salary in audit old/new values so
                                                                           * salary changes are traceable.
                                                                           */

        return snapshot;
    }

    private Long getBranchId(Staff staff) {
        return staff != null && staff.getBranch() != null ? staff.getBranch().getId() : null;
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> getStaffByBranch(Long branchId) {
        User creator = getCurrentAuthenticatedUser();
        String creatorRole = normalize(creator.getRole().getName());

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        if ("SUPER_ADMIN".equals(creatorRole)) {
            return userRepository.findAll().stream()
                    .filter(this::isStaffUser)
                    .filter(user -> belongsToBranch(user, branch.getId()))
                    .map(this::mapToStaffResponse)
                    .sorted(Comparator.comparing(StaffResponse::getId))
                    .toList();
        }

        if ("ADMIN".equals(creatorRole)) {
            Staff creatorStaff = staffRepository.findByUserId(creator.getId())
                    .orElseThrow(() -> new RuntimeException("Admin staff profile not found"));

            Long adminBranchId = creatorStaff.getBranch().getId();

            if (!adminBranchId.equals(branchId)) {
                throw new RuntimeException("ADMIN can view staff only in their own branch");
            }

            return userRepository.findAll().stream()
                    .filter(this::isStaffUser)
                    .filter(user -> belongsToBranch(user, adminBranchId))
                    .map(this::mapToStaffResponse)
                    .sorted(Comparator.comparing(StaffResponse::getId))
                    .toList();
        }

        throw new RuntimeException("Only SUPER_ADMIN or ADMIN can view staff by branch");
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> getStaffByRole(String roleName) {
        User creator = getCurrentAuthenticatedUser();
        String creatorRole = normalize(creator.getRole().getName());
        String normalizedRoleName = normalize(roleName);

        if (!isStaffRoleName(normalizedRoleName)) {
            throw new RuntimeException("Invalid staff role");
        }

        if ("SUPER_ADMIN".equals(creatorRole)) {
            return userRepository.findAll().stream()
                    .filter(this::isStaffUser)
                    .filter(user -> user.getRole() != null
                            && normalizedRoleName.equals(normalize(user.getRole().getName())))
                    .map(this::mapToStaffResponse)
                    .sorted(Comparator.comparing(StaffResponse::getId))
                    .toList();
        }

        if ("ADMIN".equals(creatorRole)) {
            Staff creatorStaff = staffRepository.findByUserId(creator.getId())
                    .orElseThrow(() -> new RuntimeException("Admin staff profile not found"));

            Long adminBranchId = creatorStaff.getBranch().getId();

            return userRepository.findAll().stream()
                    .filter(this::isStaffUser)
                    .filter(user -> user.getRole() != null
                            && normalizedRoleName.equals(normalize(user.getRole().getName())))
                    .filter(user -> belongsToBranch(user, adminBranchId))
                    .map(this::mapToStaffResponse)
                    .sorted(Comparator.comparing(StaffResponse::getId))
                    .toList();
        }

        throw new RuntimeException("Only SUPER_ADMIN or ADMIN can view staff by role");
    }

    /*
     * Decide staff salary during creation.
     * 
     * If request salary exists:
     * - use request salary.
     * 
     * If request salary does not exist:
     * - use role base salary.
     */
    private BigDecimal resolveStaffSalary(BigDecimal requestedSalary, Role role) {
        if (requestedSalary != null) {
            return normalizeSalary(requestedSalary);
        }

        return getBaseSalaryOrZero(role);
    }

    /*
     * Safely read role base salary.
     * 
     * If role salary is missing, use 0.00 instead of null.
     */
    private BigDecimal getBaseSalaryOrZero(Role role) {
        if (role == null || role.getBaseSalary() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return normalizeSalary(role.getBaseSalary());
    }

    /*
     * Salary validation helper.
     * 
     * Rules:
     * - Salary cannot be negative.
     * - Salary is stored with 2 decimal places.
     */
    private BigDecimal normalizeSalary(BigDecimal salary) {
        if (salary == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if (salary.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Salary cannot be negative");
        }

        return salary.setScale(2, RoundingMode.HALF_UP);
    }
}