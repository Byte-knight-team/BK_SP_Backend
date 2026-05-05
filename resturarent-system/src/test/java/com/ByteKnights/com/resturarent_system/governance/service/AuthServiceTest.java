package com.ByteKnights.com.resturarent_system.governance.service;

import com.ByteKnights.com.resturarent_system.auth.AuthService;
import com.ByteKnights.com.resturarent_system.dto.ChangePasswordRequest;
import com.ByteKnights.com.resturarent_system.dto.LoginResponse;
import com.ByteKnights.com.resturarent_system.dto.StaffLoginRequest;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.security.JwtService;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
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
 * Unit tests for AuthService.
 *
 * These tests cover staff login and password change logic.
 * Repositories, password encoder, JWT service, and audit logging are mocked.
 * Therefore, these tests do not connect to the real database.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private StaffRepository staffRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private JwtService jwtService;

        @Mock
        private AuditLogService auditLogService;

        @InjectMocks
        private AuthService authService;

        /**
         * Clear Spring Security context after each test.
         * This prevents one authenticated user from affecting another test.
         */
        @AfterEach
        void tearDown() {
                SecurityContextHolder.clearContext();
        }

        @Test
        void loginStaff_shouldReturnLoginResponse_whenBranchStaffCredentialsAreValid() {
                // Arrange
                Role adminRole = buildRole(1L, "ADMIN");
                User adminUser = buildUser(
                                10L,
                                "Branch Admin",
                                "admin01",
                                "admin@test.com",
                                "encoded-password",
                                adminRole,
                                true,
                                true);

                Branch branch = buildBranch(2L, "Branch 02", BranchStatus.ACTIVE);
                Staff staff = buildStaff(1L, adminUser, branch);

                StaffLoginRequest request = new StaffLoginRequest();
                request.setEmail("admin@test.com");
                request.setPassword("password123");

                when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
                when(staffRepository.findByUserId(10L)).thenReturn(Optional.of(staff));
                when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
                when(jwtService.generateToken(10L, "admin@test.com", "ADMIN", 2L, "Branch 02"))
                                .thenReturn("mock-jwt-token");

                // Act
                LoginResponse response = authService.loginStaff(request);

                // Assert
                assertNotNull(response);
                assertEquals(10L, response.getId());
                assertEquals("admin01", response.getUsername());
                assertEquals("admin@test.com", response.getEmail());
                assertEquals("ADMIN", response.getRoleName());
                assertEquals(2L, response.getBranchId());
                assertEquals("Branch 02", response.getBranchName());
                assertEquals("mock-jwt-token", response.getToken());
                assertEquals("Bearer", response.getTokenType());

                verify(auditLogService, times(1)).logActionAsUser(
                                eq(10L),
                                eq("admin@test.com"),
                                eq("ADMIN"),
                                eq(2L),
                                eq(AuditModule.AUTH),
                                eq(AuditEventType.LOGIN_SUCCESS),
                                eq(AuditStatus.SUCCESS),
                                eq(AuditSeverity.INFO),
                                eq(AuditTargetType.AUTH),
                                eq(10L),
                                eq("Staff login successful"),
                                isNull(),
                                anyMap());
        }

        @Test
        void loginStaff_shouldReturnLoginResponse_whenSuperAdminCredentialsAreValidWithoutBranch() {
                // Arrange
                Role superAdminRole = buildRole(1L, "SUPER_ADMIN");
                User superAdmin = buildUser(
                                1L,
                                "Super Admin",
                                "superadmin",
                                "superadmin@test.com",
                                "encoded-password",
                                superAdminRole,
                                true,
                                true);

                StaffLoginRequest request = new StaffLoginRequest();
                request.setEmail("superadmin@test.com");
                request.setPassword("password123");

                when(userRepository.findByEmail("superadmin@test.com")).thenReturn(Optional.of(superAdmin));
                when(staffRepository.findByUserId(1L)).thenReturn(Optional.empty());
                when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
                when(jwtService.generateToken(1L, "superadmin@test.com", "SUPER_ADMIN", null, null))
                                .thenReturn("superadmin-token");

                // Act
                LoginResponse response = authService.loginStaff(request);

                // Assert
                assertNotNull(response);
                assertEquals(1L, response.getId());
                assertEquals("SUPER_ADMIN", response.getRoleName());
                assertNull(response.getBranchId());
                assertNull(response.getBranchName());
                assertEquals("superadmin-token", response.getToken());

                verify(jwtService, times(1))
                                .generateToken(1L, "superadmin@test.com", "SUPER_ADMIN", null, null);
        }

        @Test
        void loginStaff_shouldThrowException_whenEmailDoesNotExist() {
                // Arrange
                StaffLoginRequest request = new StaffLoginRequest();
                request.setEmail("missing@test.com");
                request.setPassword("password123");

                when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

                // Act + Assert
                RuntimeException exception = assertThrows(
                                RuntimeException.class,
                                () -> authService.loginStaff(request));

                assertEquals("Invalid email", exception.getMessage());

                verify(auditLogService, times(1)).logAnonymousAction(
                                eq("missing@test.com"),
                                eq(AuditModule.AUTH),
                                eq(AuditEventType.LOGIN_FAILED),
                                eq(AuditStatus.FAILURE),
                                eq(AuditSeverity.WARN),
                                eq(AuditTargetType.AUTH),
                                isNull(),
                                eq("Staff login failed: invalid email"),
                                isNull(),
                                isNull());

                verify(jwtService, never()).generateToken(anyLong(), anyString(), anyString(), any(), any());
        }

        @Test
        void loginStaff_shouldThrowException_whenAccountIsDisabled() {
                // Arrange
                Role adminRole = buildRole(1L, "ADMIN");
                User disabledUser = buildUser(
                                10L,
                                "Branch Admin",
                                "admin01",
                                "admin@test.com",
                                "encoded-password",
                                adminRole,
                                false,
                                true);

                Branch branch = buildBranch(2L, "Branch 02", BranchStatus.ACTIVE);
                Staff staff = buildStaff(1L, disabledUser, branch);

                StaffLoginRequest request = new StaffLoginRequest();
                request.setEmail("admin@test.com");
                request.setPassword("password123");

                when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(disabledUser));
                when(staffRepository.findByUserId(10L)).thenReturn(Optional.of(staff));

                // Act + Assert
                RuntimeException exception = assertThrows(
                                RuntimeException.class,
                                () -> authService.loginStaff(request));

                assertEquals("Account disabled", exception.getMessage());

                verify(passwordEncoder, never()).matches(anyString(), anyString());
                verify(jwtService, never()).generateToken(anyLong(), anyString(), anyString(), any(), any());
        }

        @Test
        void loginStaff_shouldThrowException_whenPasswordIsWrong() {
                // Arrange
                Role adminRole = buildRole(1L, "ADMIN");
                User adminUser = buildUser(
                                10L,
                                "Branch Admin",
                                "admin01",
                                "admin@test.com",
                                "encoded-password",
                                adminRole,
                                true,
                                true);

                Branch branch = buildBranch(2L, "Branch 02", BranchStatus.ACTIVE);
                Staff staff = buildStaff(1L, adminUser, branch);

                StaffLoginRequest request = new StaffLoginRequest();
                request.setEmail("admin@test.com");
                request.setPassword("wrong-password");

                when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
                when(staffRepository.findByUserId(10L)).thenReturn(Optional.of(staff));
                when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

                // Act + Assert
                RuntimeException exception = assertThrows(
                                RuntimeException.class,
                                () -> authService.loginStaff(request));

                assertEquals("Invalid password", exception.getMessage());

                verify(jwtService, never()).generateToken(anyLong(), anyString(), anyString(), any(), any());
        }

        @Test
        void loginStaff_shouldThrowException_whenBranchIsInactive() {
                // Arrange
                Role adminRole = buildRole(1L, "ADMIN");
                User adminUser = buildUser(
                                10L,
                                "Branch Admin",
                                "admin01",
                                "admin@test.com",
                                "encoded-password",
                                adminRole,
                                true,
                                true);

                Branch inactiveBranch = buildBranch(2L, "Branch 02", BranchStatus.INACTIVE);
                Staff staff = buildStaff(1L, adminUser, inactiveBranch);

                StaffLoginRequest request = new StaffLoginRequest();
                request.setEmail("admin@test.com");
                request.setPassword("password123");

                when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
                when(staffRepository.findByUserId(10L)).thenReturn(Optional.of(staff));
                when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);

                // Act + Assert
                RuntimeException exception = assertThrows(
                                RuntimeException.class,
                                () -> authService.loginStaff(request));

                assertEquals("Your branch is inactive. Please contact the system administrator.",
                                exception.getMessage());

                verify(jwtService, never()).generateToken(anyLong(), anyString(), anyString(), any(), any());
        }

        @Test
        void changePassword_shouldUpdatePassword_whenCurrentPasswordIsCorrect() {
                // Arrange
                Role adminRole = buildRole(1L, "ADMIN");
                User adminUser = buildUser(
                                10L,
                                "Branch Admin",
                                "admin01",
                                "admin@test.com",
                                "old-encoded-password",
                                adminRole,
                                true,
                                false);

                Branch branch = buildBranch(2L, "Branch 02", BranchStatus.ACTIVE);
                Staff staff = buildStaff(1L, adminUser, branch);

                ChangePasswordRequest request = new ChangePasswordRequest();
                request.setCurrentPassword("oldPassword123");
                request.setNewPassword("newPassword123");

                setAuthenticatedUser(adminUser);

                when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
                when(staffRepository.findByUserId(10L)).thenReturn(Optional.of(staff));
                when(passwordEncoder.matches("oldPassword123", "old-encoded-password")).thenReturn(true);
                when(passwordEncoder.encode("newPassword123")).thenReturn("new-encoded-password");
                when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

                // Act
                String result = authService.changePassword(request);

                // Assert
                assertEquals("Password changed successfully", result);
                assertEquals("new-encoded-password", adminUser.getPassword());
                assertTrue(adminUser.getPasswordChanged());

                verify(userRepository, times(1)).save(adminUser);

                verify(auditLogService, times(1)).logCurrentUserAction(
                                eq(AuditModule.AUTH),
                                eq(AuditEventType.PASSWORD_CHANGED),
                                eq(AuditStatus.SUCCESS),
                                eq(AuditSeverity.INFO),
                                eq(AuditTargetType.USER),
                                eq(10L),
                                eq(2L),
                                eq("Staff password changed successfully"),
                                anyMap(),
                                anyMap());
        }

        @Test
        void changePassword_shouldThrowException_whenCurrentPasswordIsMissing() {
                // Arrange
                Role adminRole = buildRole(1L, "ADMIN");
                User adminUser = buildUser(
                                10L,
                                "Branch Admin",
                                "admin01",
                                "admin@test.com",
                                "old-encoded-password",
                                adminRole,
                                true,
                                false);

                ChangePasswordRequest request = new ChangePasswordRequest();
                request.setCurrentPassword("");
                request.setNewPassword("newPassword123");

                setAuthenticatedUser(adminUser);

                when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
                when(staffRepository.findByUserId(10L)).thenReturn(Optional.empty());

                // Act + Assert
                RuntimeException exception = assertThrows(
                                RuntimeException.class,
                                () -> authService.changePassword(request));

                assertEquals("Current password is required", exception.getMessage());

                verify(passwordEncoder, never()).matches(anyString(), anyString());
                verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void changePassword_shouldThrowException_whenCurrentPasswordIsIncorrect() {
                // Arrange
                Role adminRole = buildRole(1L, "ADMIN");
                User adminUser = buildUser(
                                10L,
                                "Branch Admin",
                                "admin01",
                                "admin@test.com",
                                "old-encoded-password",
                                adminRole,
                                true,
                                false);

                ChangePasswordRequest request = new ChangePasswordRequest();
                request.setCurrentPassword("wrongOldPassword");
                request.setNewPassword("newPassword123");

                setAuthenticatedUser(adminUser);

                when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
                when(staffRepository.findByUserId(10L)).thenReturn(Optional.empty());
                when(passwordEncoder.matches("wrongOldPassword", "old-encoded-password")).thenReturn(false);

                // Act + Assert
                RuntimeException exception = assertThrows(
                                RuntimeException.class,
                                () -> authService.changePassword(request));

                assertEquals("Current password is incorrect", exception.getMessage());

                verify(userRepository, never()).save(any(User.class));
                verify(auditLogService, never()).logCurrentUserAction(
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                anyString(),
                                any(),
                                any());
        }

        /**
         * Put a fake authenticated user into Spring Security context.
         * AuthService.changePassword reads the current user from SecurityContextHolder.
         */
        private void setAuthenticatedUser(User user) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                List.of());

                SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        private Role buildRole(Long id, String name) {
                return Role.builder()
                                .id(id)
                                .name(name)
                                .description(name + " role")
                                .build();
        }

        private User buildUser(Long id,
                        String fullName,
                        String username,
                        String email,
                        String password,
                        Role role,
                        Boolean active,
                        Boolean passwordChanged) {
                return User.builder()
                                .id(id)
                                .fullName(fullName)
                                .username(username)
                                .email(email)
                                .phone("0771234567")
                                .password(password)
                                .role(role)
                                .isActive(active)
                                .passwordChanged(passwordChanged)
                                .inviteStatus(InviteStatus.SENT)
                                .emailSent(true)
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

        private Staff buildStaff(Long id, User user, Branch branch) {
                return Staff.builder()
                                .id(id)
                                .user(user)
                                .branch(branch)
                                .salary(BigDecimal.ZERO)
                                .employmentStatus(EmploymentStatus.ACTIVE)
                                .build();
        }
}