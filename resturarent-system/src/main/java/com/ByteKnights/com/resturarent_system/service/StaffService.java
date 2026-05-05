package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.audit.Auditable;
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
     * Any role in the roles table can be used as a staff role,
     * except roles listed in NON_STAFF_ROLES.
     */
    private static final Set<String> NON_STAFF_ROLES = Set.of(
            "CUSTOMER");

    /*
     * ADMIN should not be able to create high level roles
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

    @Auditable(module = AuditModule.STAFF, eventType = AuditEventType.STAFF_CREATED, targetType = AuditTargetType.USER, description = "Staff created successfully", captureResultAsNewValue = false)
    @Transactional
    public CreateStaffResponse createStaff(CreateStaffRequest request) {

        // Normalize and validate role name
        String normalizedRoleName = normalize(request.getRoleName());

        // Check required fields and validate them
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

        // salary is optional during staff creation
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

        // Check if the email,phone number and username are already exist
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

        // salary selection rule
        BigDecimal resolvedSalary = resolveStaffSalary(request.getSalary(), role);

        User creator = getCurrentAuthenticatedUser();
        String creatorRole = normalize(creator.getRole().getName());

        Branch targetBranch = resolveTargetBranch(creator, creatorRole, normalizedRoleName, request.getBranchId());

        // generate random password for newly created staff member
        String tempPassword = generateTempPassword();

        // create user entity for newly created staff member
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

        // save user in database
        User savedUser = userRepository.save(user);

        if (!"SUPER_ADMIN".equals(normalizedRoleName)) {
            // create staff profile for newly created staff member
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

        // Send email to the new staff member
        try {
            emailService.sendStaffInviteEmail(savedUser.getEmail(), savedUser.getUsername(), tempPassword);
            savedUser.setEmailSent(true);
            savedUser.setInviteStatus(InviteStatus.SENT);

        } catch (Exception e) { // Catch any exception during email sending
            savedUser.setEmailSent(false);
            savedUser.setInviteStatus(InviteStatus.FAILED);

            // log email sending failure
            log.error(
                    "Email sending failed while creating staff. username={}, email={}",
                    savedUser.getUsername(),
                    savedUser.getEmail(),
                    e);
        }

        // save user in database after email sending
        User finalSavedUser = userRepository.save(savedUser);

        // get staff profile
        Staff finalStaff = staffRepository.findByUserId(finalSavedUser.getId()).orElse(null);

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
                // temporary password should not be null if email is not sent
                .temporaryPassword(Boolean.TRUE.equals(finalSavedUser.getEmailSent()) ? null : tempPassword)
                .branchId(targetBranch != null ? targetBranch.getId() : null)
                .branchName(targetBranch != null ? targetBranch.getName() : null)
                .message(
                        Boolean.TRUE.equals(finalSavedUser.getEmailSent()) ? "Email sent successfully" : "Email failed")
                .build();
    }

    @Auditable(module = AuditModule.STAFF, eventType = AuditEventType.INVITE_RESENT, targetType = AuditTargetType.USER, description = "Staff invite resent successfully", captureResultAsNewValue = false)
    @Transactional
    // resend invite for a staff member if it has been failed
    public CreateStaffResponse resendInvite(Long userId) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User creator = getCurrentAuthenticatedUser();
        String creatorRole = normalize(creator.getRole().getName());
        String targetRole = normalize(targetUser.getRole().getName());

        Staff targetStaff = staffRepository.findByUserId(userId).orElse(null);

        // SUPER_ADMIN can resend for anyone in staff flow
        if ("SUPER_ADMIN".equals(creatorRole)) {
        } else if ("ADMIN".equals(creatorRole)) {
            if (targetStaff == null) {
                throw new RuntimeException("ADMIN cannot resend invite for this user");
            }

            // ADMIN can resend only for staff in their own branch
            Staff creatorStaff = staffRepository.findByUserId(creator.getId())
                    .orElseThrow(() -> new RuntimeException("Admin staff profile not found"));

            // check if the target user is not ADMIN and not SUPER_ADMIN
            if ("ADMIN".equals(targetRole) || "SUPER_ADMIN".equals(targetRole)) {
                throw new RuntimeException("ADMIN can resend invite only for non-admin staff");
            }

            Long adminBranchId = creatorStaff.getBranch().getId();
            Long targetBranchId = targetStaff.getBranch().getId();

            // check ADMIN branch equals to new staff member branch
            if (!adminBranchId.equals(targetBranchId)) {
                throw new RuntimeException("ADMIN can resend invite only for staff in their own branch");
            }

            // check the new staff member is a Admin creatable role
            if (!isAdminCreatableRoleName(targetRole)) {
                throw new RuntimeException("ADMIN can resend invite only for MANAGER, CHEF, RECEPTIONIST, or DELIVERY");
            }
            // Only super admin or admin can send resend invite

        } else {
            throw new RuntimeException("Only SUPER_ADMIN or ADMIN can resend invites");
        }

        // Generate new temporary password
        String tempPassword = generateTempPassword();

        // Hash and save the new temporary password
        targetUser.setPassword(passwordEncoder.encode(tempPassword));

        // Remove the temporary password because it is no longer needed
        targetUser.setTemporaryPassword(null);

        // Set password changed to false because the user has not changed the password
        // yet
        targetUser.setPasswordChanged(false);

        // Set invite status to pending because the user has not accepted the invite yet
        targetUser.setInviteStatus(InviteStatus.PENDING);

        // Send email to the new staff member
        try {
            emailService.sendStaffInviteEmail(targetUser.getEmail(), targetUser.getUsername(), tempPassword);
            targetUser.setEmailSent(true);
            targetUser.setInviteStatus(InviteStatus.SENT);
        } catch (Exception e) {
            targetUser.setEmailSent(false);
            targetUser.setInviteStatus(InviteStatus.FAILED);

            // log email sending failure
            log.error(
                    "Email sending failed while resending invite. userId={}, username={}, email={}",
                    targetUser.getId(),
                    targetUser.getUsername(),
                    targetUser.getEmail(),
                    e);
        }

        // save user in database after email sending
        User savedUser = userRepository.save(targetUser);

        // get staff profile
        Staff savedStaff = staffRepository.findByUserId(savedUser.getId()).orElse(null);

        // return response
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

    // Gets the staff list based on the logged-in user's role.
    @Transactional(readOnly = true)
    public List<StaffResponse> getAllStaff() {
        User creator = getCurrentAuthenticatedUser();
        String creatorRole = normalize(creator.getRole().getName());

        // SUPER_ADMIN has global access, so return all staff users.
        if ("SUPER_ADMIN".equals(creatorRole)) {
            return userRepository.findAll().stream()
                    .filter(this::isStaffUser)
                    .map(this::mapToStaffResponse)
                    .sorted(Comparator.comparing(StaffResponse::getId))
                    .toList();
        }

        /*
            ADMIN is branch-scoped.
            First get the ADMIN's staff profile to find their assigned branch.
        */
        if ("ADMIN".equals(creatorRole)) {
            Staff creatorStaff = staffRepository.findByUserId(creator.getId())
                    .orElseThrow(() -> new RuntimeException("Admin staff profile not found"));

            Long adminBranchId = creatorStaff.getBranch().getId();

            //Return only staff users who belong to the same branch as the ADMIN
            return userRepository.findAll().stream()
                    .filter(this::isStaffUser)
                    .filter(user -> belongsToBranch(user, adminBranchId))
                    .map(this::mapToStaffResponse)
                    //Sort the results by ID for consistent ordering and convert to list
                    .sorted(Comparator.comparing(StaffResponse::getId))
                    .toList();
        }

        throw new RuntimeException("Only SUPER_ADMIN or ADMIN can view staff");
    }

    // Gets a staff member by their user ID.
    @Transactional(readOnly = true)
    public StaffResponse getStaffById(Long userId) {
        User creator = getCurrentAuthenticatedUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Staff user not found"));

        if (!isStaffUser(targetUser)) {
            throw new RuntimeException("This user is not a staff user");
        }
        //ensure only ADMIN and SUPER ADMIN can view staff members, and ADMIN can only view staff members in their own branch.
        ensureCanViewTarget(creator, targetUser);

        return mapToStaffResponse(targetUser);
    }

    //Updates an existing staff member's details. The caller must have permission to
    //manage the target staff member.
    @Transactional
    public StaffResponse updateStaff(Long userId, UpdateStaffRequest request) {
        User creator = getCurrentAuthenticatedUser();
        String creatorRole = normalize(creator.getRole().getName());

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Staff user not found"));

        //Check if the target user is a staff user
        if (!isStaffUser(targetUser)) {
            throw new RuntimeException("This user is not a staff user");
        }

        //ensure the caller has permission to manage the target staff member
        ensureCanManageTarget(creator, targetUser);

        String currentTargetRole = normalize(targetUser.getRole().getName());
        /*
         * These values help us decide whether to update salary from the new role
         * default.
         */
        boolean roleChanged = false;
        Role selectedRoleForSalary = targetUser.getRole();
        Staff targetStaff = staffRepository.findByUserId(targetUser.getId()).orElse(null);
        //build audit snapshot for logging
        Map<String, Object> oldValues = buildStaffAuditSnapshot(targetUser, targetStaff);

        // Update full name
        if (request.getFullName() != null) {
            String fullName = request.getFullName().trim();
            if (fullName.isEmpty()) {
                throw new RuntimeException("Full name cannot be empty");
            }
            targetUser.setFullName(fullName);
        }

        //Update email
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

        //Update phone number
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

        //Update role name
        if (request.getRoleName() != null) {
            String newRoleName = normalize(request.getRoleName());

            //Validate the new role using database driven staff role rules
            if (!isStaffRoleName(newRoleName)) {
                throw new RuntimeException("Only staff roles can be assigned here");
            }

            //Check if the role is changed
            if (!newRoleName.equals(currentTargetRole)) {
                //Check if the role is changed to or from SUPER_ADMIN
                if ("SUPER_ADMIN".equals(currentTargetRole) || "SUPER_ADMIN".equals(newRoleName)) {
                    throw new RuntimeException("Changing to or from SUPER_ADMIN through update is not supported yet");
                }

                //ADMIN can assign only branch-level staff roles
                if ("ADMIN".equals(creatorRole) && !isAdminCreatableRoleName(newRoleName)) {
                    throw new RuntimeException("ADMIN can assign only branch-level staff roles");
                }

                Role newRole = roleRepository.findByName(newRoleName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + newRoleName));

                targetUser.setRole(newRole);

                /*
                 * Mark that role changed.
                 */
                roleChanged = true;
                selectedRoleForSalary = newRole;
            }
        }

        //Update individual staff salary
        //If request.salary is provided, use that value.
        //If role changed and request.salary is not provided, copy the new role base salary.
        //If neither happened, keep existing salary unchanged.
        if (targetStaff != null) {
            if (request.getSalary() != null) {
                targetStaff.setSalary(normalizeSalary(request.getSalary()));
            } else if (roleChanged) {
                targetStaff.setSalary(getBaseSalaryOrZero(selectedRoleForSalary));
            }
        }

        //Update individual staff branch
        //If request.branchId is provided, use that value.
        if (targetStaff != null && request.getBranchId() != null) {

            // SUPER_ADMIN can assign staff to any branch.
            if ("SUPER_ADMIN".equals(creatorRole)) {
                
                Branch newBranch = branchRepository.findByIdAndStatus(request.getBranchId(), BranchStatus.ACTIVE)
                        .orElseThrow(() -> new RuntimeException("Active branch not found"));
                targetStaff.setBranch(newBranch);

            
            } else if ("ADMIN".equals(creatorRole)) {
                // Ensure that the creator is an admin
                Staff creatorStaff = staffRepository.findByUserId(creator.getId())
                        .orElseThrow(() -> new RuntimeException("Admin staff profile not found"));

                Long adminBranchId = creatorStaff.getBranch().getId();

                //ADMIN can assign only branch-level staff roles
                if (!request.getBranchId().equals(adminBranchId)) {
                    throw new RuntimeException("ADMIN can assign staff only to their own branch");
                }

                targetStaff.setBranch(creatorStaff.getBranch());
            }
        }

        //save the new staff member details
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

    @Auditable(module = AuditModule.STAFF, eventType = AuditEventType.STAFF_ACTIVATED, targetType = AuditTargetType.USER, description = "Staff activated successfully", captureResultAsNewValue = false)
    @Transactional
    //activate a staff member
    public StaffResponse activateStaff(Long userId) {
        User creator = getCurrentAuthenticatedUser();

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Staff user not found"));

        //Ensure that the target user is a staff user
        if (!isStaffUser(targetUser)) {
            throw new RuntimeException("This user is not a staff user");
        }

        ensureCanManageTarget(creator, targetUser);

        //activate the staff member
        targetUser.setIsActive(true);
        User savedUser = userRepository.save(targetUser);

        return mapToStaffResponse(savedUser);
    }

    @Auditable(module = AuditModule.STAFF, eventType = AuditEventType.STAFF_DEACTIVATED, targetType = AuditTargetType.USER, description = "Staff deactivated successfully", captureResultAsNewValue = false)
    @Transactional
    //deactivate a staff member
    public StaffResponse deactivateStaff(Long userId) {
        User creator = getCurrentAuthenticatedUser();

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Staff user not found"));

        if (!isStaffUser(targetUser)) {
            throw new RuntimeException("This user is not a staff user");
        }

        // Ensure that the creator can manage the target user
        ensureCanManageTarget(creator, targetUser);

        //deactivate the staff member
        targetUser.setIsActive(false);
        User savedUser = userRepository.save(targetUser);

        return mapToStaffResponse(savedUser);
    }

    //resolve the target branch
    private Branch resolveTargetBranch(User creator, String creatorRole, String targetRole, Long requestedBranchId) {

        if ("SUPER_ADMIN".equals(creatorRole)) {
            if ("SUPER_ADMIN".equals(targetRole)) {
                return null;
            }

            return branchRepository.findByIdAndStatus(requestedBranchId, BranchStatus.ACTIVE)
                    .orElseThrow(() -> new RuntimeException("Active branch not found"));
        }

        //resolve the target branch for ADMIN
        if ("ADMIN".equals(creatorRole)) {

            //ADMIN can create only branch-level staff roles
            if (!isAdminCreatableRoleName(targetRole)) {
                throw new RuntimeException("ADMIN can create only MANAGER, CHEF, RECEPTIONIST, DELIVERY or Low Level Staff Members");
            }

            Staff creatorStaff = staffRepository.findByUserId(creator.getId())
                    .orElseThrow(() -> new RuntimeException("Admin staff profile not found"));

            Branch adminBranch = creatorStaff.getBranch();

            //ADMIN can create staff only for their own branch
            if (requestedBranchId != null && !requestedBranchId.equals(adminBranch.getId())) {
                throw new RuntimeException("ADMIN can create staff only for their own branch");
            }

            return adminBranch;
        }

        throw new RuntimeException("Only SUPER_ADMIN or ADMIN can create staff");
    }

    //resolve the current authenticated user
    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        //first check if the user in security context
        if (authentication == null) {
            throw new RuntimeException("Authenticated user not found");
        }

        Object principal = authentication.getPrincipal();

        //If not in JWT token
        if (principal instanceof JwtUserPrincipal jwtUser) {
            return userRepository.findByEmail(jwtUser.getEmail())
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        }

        //Finally in database
        if (principal instanceof User user) {
            return userRepository.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        }

        //Lastly check using the email address
        String email = authentication.getName();
        if (email != null && !email.trim().isEmpty()) {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        }

        throw new RuntimeException("Authenticated user not found");
    }

    //generate a random temporary password for the newly created staff member
    private String generateTempPassword() {
        int length = 10;
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(length);

        //Loop runs 10 times for each random character
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }

        //create the temporary password string
        return sb.toString();
    }

    // validate the email address
    private boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    //validate the phone number
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
     */
    private boolean isStaffUser(User user) {
        return user.getRole() != null
                && isStaffRoleName(user.getRole().getName());
    }

    /*
     * Checks whether a role name is allowed to be used as a staff role.
     */
    private boolean isStaffRoleName(String roleName) {
        String normalizedRoleName = normalize(roleName);

        return normalizedRoleName != null
                && !NON_STAFF_ROLES.contains(normalizedRoleName);
    }

    /*
     * Checks whether ADMIN can create/manage this role.
     */
    private boolean isAdminCreatableRoleName(String roleName) {
        String normalizedRoleName = normalize(roleName);

        return normalizedRoleName != null
                && !ADMIN_BLOCKED_CREATION_ROLES.contains(normalizedRoleName);
    }

    //check if the user belongs to a branch
    private boolean belongsToBranch(User user, Long branchId) {
        return staffRepository.findByUserId(user.getId())
                .map(staff -> staff.getBranch() != null && staff.getBranch().getId().equals(branchId))
                .orElse(false);
    }

    //check if the creator can view the target user
    private void ensureCanViewTarget(User creator, User targetUser) {
        String creatorRole = normalize(creator.getRole().getName());

        // SUPER_ADMIN can view all staff
        if ("SUPER_ADMIN".equals(creatorRole)) {
            return;
        }

        // ADMIN can view only branch-linked staff in their own branch
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

    //check if the creator can manage the target user
    private void ensureCanManageTarget(User creator, User targetUser) {
        String creatorRole = normalize(creator.getRole().getName());
        String targetRole = normalize(targetUser.getRole().getName());

        // SUPER_ADMIN can manage all staff
        if ("SUPER_ADMIN".equals(creatorRole)) {
            return;
        }

        // ADMIN can manage only branch linked staff in their own branch
        if ("ADMIN".equals(creatorRole)) {
            if (!isAdminCreatableRoleName(targetRole)) {
                throw new RuntimeException("ADMIN can manage only MANAGER, CHEF, RECEPTIONIST, DELIVERY or Low level staff members.");
            }

            Staff creatorStaff = staffRepository.findByUserId(creator.getId())
                    .orElseThrow(() -> new RuntimeException("Admin staff profile not found"));

            Staff targetStaff = staffRepository.findByUserId(targetUser.getId())
                    .orElseThrow(() -> new RuntimeException("ADMIN can manage only branch linked staff"));

            Long adminBranchId = creatorStaff.getBranch().getId();
            Long targetBranchId = targetStaff.getBranch().getId();

            if (!adminBranchId.equals(targetBranchId)) {
                throw new RuntimeException("ADMIN can manage staff only in their own branch");
            }

            return;
        }

        throw new RuntimeException("Only SUPER_ADMIN or ADMIN can manage staff");
    }

    //map user to staff response
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

    // This snapshot is used to create an audit log of the staff user and info
    // snapshot is similar to a backup of the staff user and their staff information
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
        snapshot.put("salary", staff != null ? staff.getSalary() : null);

        return snapshot;
    }

    //get branch id from staff
    private Long getBranchId(Staff staff) {
        return staff != null && staff.getBranch() != null ? staff.getBranch().getId() : null;
    }

    //get staff by branch and view staff members
    @Transactional(readOnly = true)
    public List<StaffResponse> getStaffByBranch(Long branchId) {
        User creator = getCurrentAuthenticatedUser();
        String creatorRole = normalize(creator.getRole().getName());

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        //SUPER_ADMIN can view all staff
        if ("SUPER_ADMIN".equals(creatorRole)) {
            return userRepository.findAll().stream()
                    .filter(this::isStaffUser)
                    .filter(user -> belongsToBranch(user, branch.getId()))
                    .map(this::mapToStaffResponse)
                    .sorted(Comparator.comparing(StaffResponse::getId))
                    .toList();
        }

        //ADMIN can view only staff in their own branch
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

    //get staff by role and view staff members
    @Transactional(readOnly = true)
    public List<StaffResponse> getStaffByRole(String roleName) {
        User creator = getCurrentAuthenticatedUser();
        String creatorRole = normalize(creator.getRole().getName());
        String normalizedRoleName = normalize(roleName);

        if (!isStaffRoleName(normalizedRoleName)) {
            throw new RuntimeException("Invalid staff role");
        }

        //SUPER_ADMIN can view all staff by role
        if ("SUPER_ADMIN".equals(creatorRole)) {
            return userRepository.findAll().stream()
                    .filter(this::isStaffUser)
                    .filter(user -> user.getRole() != null
                            && normalizedRoleName.equals(normalize(user.getRole().getName())))
                    .map(this::mapToStaffResponse)
                    .sorted(Comparator.comparing(StaffResponse::getId))
                    .toList();
        }

        //ADMIN can view only staff in their own branch by role
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
     */
    private BigDecimal resolveStaffSalary(BigDecimal requestedSalary, Role role) {
        if (requestedSalary != null) {
            return normalizeSalary(requestedSalary);
        }
        return getBaseSalaryOrZero(role);
    }

    //Safely read role base salary.
    //If role salary is missing, use 0.00 instead of null.
    private BigDecimal getBaseSalaryOrZero(Role role) {
        if (role == null || role.getBaseSalary() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return normalizeSalary(role.getBaseSalary());
    }

    /*
     * Salary validation helper.
     * Rules - Salary cannot be negative and Salary is stored with 2 decimal places.
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