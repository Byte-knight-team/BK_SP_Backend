package com.ByteKnights.com.resturarent_system.governance.service;

import com.ByteKnights.com.resturarent_system.dto.CreateStaffRequest;
import com.ByteKnights.com.resturarent_system.dto.CreateStaffResponse;
import com.ByteKnights.com.resturarent_system.dto.StaffResponse;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import com.ByteKnights.com.resturarent_system.repository.RoleRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.StaffService;
import com.ByteKnights.com.resturarent_system.service.email.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StaffService.
 *
 * These tests are for the governance / staff management module.
 * Repositories, email service, password encoder, and audit log service are mocked.
 * Therefore, these tests do not connect to the real database or send real emails.
 */
@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private StaffService staffService;

    /**
     * Clear Spring Security context after each test.
     * This prevents one test's authenticated user from affecting another test.
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createStaff_shouldCreateBranchStaff_whenSuperAdminCreatesValidStaff() {
        // Arrange
        Role superAdminRole = buildRole(1L, "SUPER_ADMIN", BigDecimal.ZERO);
        Role managerRole = buildRole(2L, "MANAGER", new BigDecimal("75000.00"));

        User superAdmin = buildUser(
                100L,
                "Super Admin",
                "superadmin",
                "superadmin@test.com",
                "0770000000",
                superAdminRole,
                true
        );

        Branch branch = buildBranch(1L, "Main Branch", BranchStatus.ACTIVE);

        CreateStaffRequest request = new CreateStaffRequest();
        request.setFullName("Test Manager");
        request.setUsername("manager01");
        request.setEmail("manager01@test.com");
        request.setPhone("0771234567");
        request.setRoleName("MANAGER");
        request.setBranchId(1L);

        setAuthenticatedUser(superAdmin);

        when(userRepository.findById(100L)).thenReturn(Optional.of(superAdmin));
        when(userRepository.existsByEmail("manager01@test.com")).thenReturn(false);
        when(userRepository.existsByPhone("0771234567")).thenReturn(false);
        when(userRepository.existsByUsername("manager01")).thenReturn(false);
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.of(managerRole));
        when(branchRepository.findByIdAndStatus(1L, BranchStatus.ACTIVE)).thenReturn(Optional.of(branch));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-temp-password");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User userToSave = invocation.getArgument(0);

            if (userToSave.getId() == null) {
                userToSave.setId(10L);
            }

            return userToSave;
        });

        Staff savedStaff = buildStaff(1L, null, branch, new BigDecimal("75000.00"));
        when(staffRepository.findByUserId(10L)).thenReturn(Optional.of(savedStaff));

        // Act
        CreateStaffResponse response = staffService.createStaff(request);

        // Assert
        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Test Manager", response.getFullName());
        assertEquals("manager01", response.getUsername());
        assertEquals("manager01@test.com", response.getEmail());
        assertEquals("0771234567", response.getPhone());
        assertEquals("MANAGER", response.getRoleName());
        assertEquals(1L, response.getBranchId());
        assertEquals("Main Branch", response.getBranchName());
        assertEquals(InviteStatus.SENT, response.getInviteStatus());
        assertTrue(response.getEmailSent());
        assertNull(response.getTemporaryPassword());
        assertEquals("Email sent successfully", response.getMessage());

        verify(userRepository, times(2)).save(any(User.class));
        verify(staffRepository, times(1)).save(any(Staff.class));
        verify(emailService, times(1))
                .sendStaffInviteEmail(eq("manager01@test.com"), eq("manager01"), anyString());

    }

    @Test
    void createStaff_shouldReturnTemporaryPassword_whenEmailSendingFails() {
        // Arrange
        Role superAdminRole = buildRole(1L, "SUPER_ADMIN", BigDecimal.ZERO);
        Role chefRole = buildRole(2L, "CHEF", new BigDecimal("65000.00"));

        User superAdmin = buildUser(
                100L,
                "Super Admin",
                "superadmin",
                "superadmin@test.com",
                "0770000000",
                superAdminRole,
                true
        );

        Branch branch = buildBranch(1L, "Main Branch", BranchStatus.ACTIVE);

        CreateStaffRequest request = new CreateStaffRequest();
        request.setFullName("Test Chef");
        request.setUsername("chef01");
        request.setEmail("chef01@test.com");
        request.setPhone("0771111111");
        request.setRoleName("CHEF");
        request.setBranchId(1L);

        setAuthenticatedUser(superAdmin);

        when(userRepository.findById(100L)).thenReturn(Optional.of(superAdmin));
        when(userRepository.existsByEmail("chef01@test.com")).thenReturn(false);
        when(userRepository.existsByPhone("0771111111")).thenReturn(false);
        when(userRepository.existsByUsername("chef01")).thenReturn(false);
        when(roleRepository.findByName("CHEF")).thenReturn(Optional.of(chefRole));
        when(branchRepository.findByIdAndStatus(1L, BranchStatus.ACTIVE)).thenReturn(Optional.of(branch));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-temp-password");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User userToSave = invocation.getArgument(0);

            if (userToSave.getId() == null) {
                userToSave.setId(11L);
            }

            return userToSave;
        });

        Staff savedStaff = buildStaff(2L, null, branch, new BigDecimal("65000.00"));
        when(staffRepository.findByUserId(11L)).thenReturn(Optional.of(savedStaff));

        doThrow(new RuntimeException("SMTP failed"))
                .when(emailService)
                .sendStaffInviteEmail(eq("chef01@test.com"), eq("chef01"), anyString());

        // Act
        CreateStaffResponse response = staffService.createStaff(request);

        // Assert
        assertNotNull(response);
        assertEquals(11L, response.getId());
        assertEquals("chef01@test.com", response.getEmail());
        assertEquals(InviteStatus.FAILED, response.getInviteStatus());
        assertFalse(response.getEmailSent());
        assertNotNull(response.getTemporaryPassword());
        assertEquals("Email failed", response.getMessage());

        verify(userRepository, times(2)).save(any(User.class));
        verify(staffRepository, times(1)).save(any(Staff.class));
        verify(emailService, times(1))
                .sendStaffInviteEmail(eq("chef01@test.com"), eq("chef01"), anyString());
    }

    @Test
    void createStaff_shouldThrowException_whenRequiredFieldsAreInvalid() {
        // Arrange
        CreateStaffRequest request = new CreateStaffRequest();
        request.setFullName("");
        request.setUsername("");
        request.setEmail("wrong-email");
        request.setPhone("123");
        request.setRoleName("");
        request.setBranchId(null);

        // Act + Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> staffService.createStaff(request)
        );

        assertTrue(exception.getMessage().contains("Full name is required"));
        assertTrue(exception.getMessage().contains("Username is required"));
        assertTrue(exception.getMessage().contains("Invalid email format"));
        assertTrue(exception.getMessage().contains("Phone number must be exactly 10 digits"));
        assertTrue(exception.getMessage().contains("Role is required"));

        verifyNoInteractions(userRepository);
        verifyNoInteractions(roleRepository);
        verifyNoInteractions(branchRepository);
        verifyNoInteractions(staffRepository);
        verifyNoInteractions(emailService);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void createStaff_shouldThrowException_whenEmailAlreadyExists() {
        // Arrange
        CreateStaffRequest request = new CreateStaffRequest();
        request.setFullName("Test Manager");
        request.setUsername("manager01");
        request.setEmail("manager01@test.com");
        request.setPhone("0771234567");
        request.setRoleName("MANAGER");
        request.setBranchId(1L);

        when(userRepository.existsByEmail("manager01@test.com")).thenReturn(true);
        when(userRepository.existsByPhone("0771234567")).thenReturn(false);
        when(userRepository.existsByUsername("manager01")).thenReturn(false);

        // Act + Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> staffService.createStaff(request)
        );

        assertTrue(exception.getMessage().contains("Email already exists"));

        verify(userRepository, times(1)).existsByEmail("manager01@test.com");
        verify(userRepository, times(1)).existsByPhone("0771234567");
        verify(userRepository, times(1)).existsByUsername("manager01");
        verifyNoInteractions(roleRepository);
        verifyNoInteractions(branchRepository);
        verifyNoInteractions(staffRepository);
        verifyNoInteractions(emailService);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void createStaff_shouldThrowException_whenSuperAdminUsesInactiveOrMissingBranch() {
        // Arrange
        Role superAdminRole = buildRole(1L, "SUPER_ADMIN", BigDecimal.ZERO);
        Role managerRole = buildRole(2L, "MANAGER", new BigDecimal("75000.00"));

        User superAdmin = buildUser(
                100L,
                "Super Admin",
                "superadmin",
                "superadmin@test.com",
                "0770000000",
                superAdminRole,
                true
        );

        CreateStaffRequest request = new CreateStaffRequest();
        request.setFullName("Test Manager");
        request.setUsername("manager01");
        request.setEmail("manager01@test.com");
        request.setPhone("0771234567");
        request.setRoleName("MANAGER");
        request.setBranchId(99L);

        setAuthenticatedUser(superAdmin);

        when(userRepository.existsByEmail("manager01@test.com")).thenReturn(false);
        when(userRepository.existsByPhone("0771234567")).thenReturn(false);
        when(userRepository.existsByUsername("manager01")).thenReturn(false);
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.of(managerRole));
        when(userRepository.findById(100L)).thenReturn(Optional.of(superAdmin));
        when(branchRepository.findByIdAndStatus(99L, BranchStatus.ACTIVE)).thenReturn(Optional.empty());

        // Act + Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> staffService.createStaff(request)
        );

        assertEquals("Active branch not found", exception.getMessage());

        verify(branchRepository, times(1)).findByIdAndStatus(99L, BranchStatus.ACTIVE);
        verify(userRepository, never()).save(any(User.class));
        verify(staffRepository, never()).save(any(Staff.class));
        verifyNoInteractions(emailService);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void createStaff_shouldThrowException_whenAdminCreatesStaffForAnotherBranch() {
        // Arrange
        Role adminRole = buildRole(1L, "ADMIN", BigDecimal.ZERO);
        Role managerRole = buildRole(2L, "MANAGER", new BigDecimal("75000.00"));

        Branch adminBranch = buildBranch(1L, "Admin Branch", BranchStatus.ACTIVE);
        Branch requestedBranch = buildBranch(2L, "Other Branch", BranchStatus.ACTIVE);

        User admin = buildUser(
                200L,
                "Branch Admin",
                "admin01",
                "admin01@test.com",
                "0770000001",
                adminRole,
                true
        );

        Staff adminStaff = buildStaff(10L, admin, adminBranch, BigDecimal.ZERO);

        CreateStaffRequest request = new CreateStaffRequest();
        request.setFullName("Test Manager");
        request.setUsername("manager02");
        request.setEmail("manager02@test.com");
        request.setPhone("0772222222");
        request.setRoleName("MANAGER");
        request.setBranchId(requestedBranch.getId());

        setAuthenticatedUser(admin);

        when(userRepository.existsByEmail("manager02@test.com")).thenReturn(false);
        when(userRepository.existsByPhone("0772222222")).thenReturn(false);
        when(userRepository.existsByUsername("manager02")).thenReturn(false);
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.of(managerRole));
        when(userRepository.findById(200L)).thenReturn(Optional.of(admin));
        when(staffRepository.findByUserId(200L)).thenReturn(Optional.of(adminStaff));

        // Act + Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> staffService.createStaff(request)
        );

        assertEquals("ADMIN can create staff only for their own branch", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verify(staffRepository, never()).save(any(Staff.class));
        verifyNoInteractions(emailService);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void deactivateStaff_shouldSetStaffUserInactive_whenSuperAdminManagesStaff() {
        // Arrange
        Role superAdminRole = buildRole(1L, "SUPER_ADMIN", BigDecimal.ZERO);
        Role managerRole = buildRole(2L, "MANAGER", new BigDecimal("75000.00"));

        User superAdmin = buildUser(
                100L,
                "Super Admin",
                "superadmin",
                "superadmin@test.com",
                "0770000000",
                superAdminRole,
                true
        );

        User targetUser = buildUser(
                10L,
                "Test Manager",
                "manager01",
                "manager01@test.com",
                "0771234567",
                managerRole,
                true
        );

        Branch branch = buildBranch(1L, "Main Branch", BranchStatus.ACTIVE);
        Staff targetStaff = buildStaff(1L, targetUser, branch, new BigDecimal("75000.00"));

        setAuthenticatedUser(superAdmin);

        when(userRepository.findById(100L)).thenReturn(Optional.of(superAdmin));
        when(userRepository.findById(10L)).thenReturn(Optional.of(targetUser));
        when(staffRepository.findByUserId(10L)).thenReturn(Optional.of(targetStaff));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        StaffResponse response = staffService.deactivateStaff(10L);

        // Assert
        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertFalse(response.getActive());
        assertEquals("MANAGER", response.getRoleName());
        assertEquals(1L, response.getBranchId());

        verify(userRepository, times(1)).save(targetUser);
    }

    /**
     * Put a fake authenticated user into Spring Security context.
     * StaffService uses SecurityContextHolder to find who is performing the action.
     */
    private void setAuthenticatedUser(User user) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Role buildRole(Long id, String name, BigDecimal baseSalary) {
        return Role.builder()
                .id(id)
                .name(name)
                .description(name + " role")
                .baseSalary(baseSalary)
                .build();
    }

    private User buildUser(Long id,
                           String fullName,
                           String username,
                           String email,
                           String phone,
                           Role role,
                           Boolean active) {
        return User.builder()
                .id(id)
                .fullName(fullName)
                .username(username)
                .email(email)
                .phone(phone)
                .password("encoded-password")
                .role(role)
                .isActive(active)
                .passwordChanged(false)
                .inviteStatus(InviteStatus.PENDING)
                .emailSent(false)
                .build();
    }

    private Branch buildBranch(Long id, String name, BranchStatus status) {
        return Branch.builder()
                .id(id)
                .name(name)
                .address("Test Address")
                .contactNumber("0779999999")
                .email("branch@test.com")
                .status(status)
                .build();
    }

    private Staff buildStaff(Long id, User user, Branch branch, BigDecimal salary) {
        return Staff.builder()
                .id(id)
                .user(user)
                .branch(branch)
                .salary(salary)
                .employmentStatus(EmploymentStatus.ACTIVE)
                .build();
    }
}