package com.ByteKnights.com.resturarent_system.governance.service;

import com.ByteKnights.com.resturarent_system.dto.request.superadmin.UpdateBranchConfigRequest;
import com.ByteKnights.com.resturarent_system.dto.request.superadmin.UpdateGlobalConfigRequest;
import com.ByteKnights.com.resturarent_system.dto.response.superadmin.BranchConfigResponse;
import com.ByteKnights.com.resturarent_system.dto.response.superadmin.EffectiveBranchConfigResponse;
import com.ByteKnights.com.resturarent_system.dto.response.superadmin.GlobalConfigResponse;
import com.ByteKnights.com.resturarent_system.entity.AuditEventType;
import com.ByteKnights.com.resturarent_system.entity.AuditModule;
import com.ByteKnights.com.resturarent_system.entity.AuditSeverity;
import com.ByteKnights.com.resturarent_system.entity.AuditStatus;
import com.ByteKnights.com.resturarent_system.entity.AuditTargetType;
import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.entity.BranchConfig;
import com.ByteKnights.com.resturarent_system.entity.BranchStatus;
import com.ByteKnights.com.resturarent_system.entity.InviteStatus;
import com.ByteKnights.com.resturarent_system.entity.Role;
import com.ByteKnights.com.resturarent_system.entity.SystemConfig;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.repository.BranchConfigRepository;
import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import com.ByteKnights.com.resturarent_system.repository.SystemConfigRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.SystemConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/*
 * Unit tests for SystemConfigService.
 *
 * Current review scope:
 * - SUPER_ADMIN only access
 * - global config
 * - branch config
 * - effective config
 * - audit logging for config updates
 *
 * Operating hours were removed from this scope.
 */
@ExtendWith(MockitoExtension.class)
class SystemConfigServiceTest {

    @Mock
    private SystemConfigRepository systemConfigRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private BranchConfigRepository branchConfigRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private SystemConfigService systemConfigService;

    /*
     * Clear Spring Security context after each test.
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getGlobalConfig_shouldCreateDefaultConfig_whenNoConfigExists() {
        // Arrange
        User superAdmin = buildSuperAdminUser();
        setAuthenticatedUser(superAdmin);

        when(userRepository.findById(1L)).thenReturn(Optional.of(superAdmin));
        when(systemConfigRepository.findAll()).thenReturn(List.of());

        when(systemConfigRepository.save(any(SystemConfig.class))).thenAnswer(invocation -> {
            SystemConfig configToSave = invocation.getArgument(0);
            configToSave.setId(1L);
            return configToSave;
        });

        // Act
        GlobalConfigResponse response = systemConfigService.getGlobalConfig();

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertFalse(response.isTaxEnabled());
        assertEquals(BigDecimal.ZERO, response.getTaxPercentage());
        assertFalse(response.isServiceChargeEnabled());
        assertEquals(BigDecimal.ZERO, response.getServiceChargePercentage());
        assertFalse(response.isLoyaltyEnabled());
        assertEquals(BigDecimal.ZERO, response.getPointsPerAmount());
        assertEquals(BigDecimal.ONE, response.getAmountPerPoint());
        assertEquals(0, response.getMinPointsToRedeem());
        assertEquals(BigDecimal.ZERO, response.getValuePerPoint());

        verify(systemConfigRepository, times(1)).findAll();
        verify(systemConfigRepository, times(1)).save(any(SystemConfig.class));
    }

    @Test
    void updateGlobalConfig_shouldUpdateConfigAndWriteAuditLog_whenRequestIsValid() {
        // Arrange
        User superAdmin = buildSuperAdminUser();
        SystemConfig existingConfig = buildSystemConfig(1L);
        UpdateGlobalConfigRequest request = buildValidGlobalConfigRequest();

        setAuthenticatedUser(superAdmin);

        when(userRepository.findById(1L)).thenReturn(Optional.of(superAdmin));
        when(systemConfigRepository.findAll()).thenReturn(List.of(existingConfig));
        when(systemConfigRepository.save(any(SystemConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        GlobalConfigResponse response = systemConfigService.updateGlobalConfig(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());

        assertTrue(response.isTaxEnabled());
        assertEquals(new BigDecimal("10.00"), response.getTaxPercentage());

        assertTrue(response.isServiceChargeEnabled());
        assertEquals(new BigDecimal("5.00"), response.getServiceChargePercentage());

        assertTrue(response.isLoyaltyEnabled());
        assertEquals(new BigDecimal("1.00"), response.getPointsPerAmount());
        assertEquals(new BigDecimal("100.00"), response.getAmountPerPoint());
        assertEquals(50, response.getMinPointsToRedeem());
        assertEquals(new BigDecimal("2.00"), response.getValuePerPoint());

        verify(systemConfigRepository, times(1)).save(existingConfig);

        verify(auditLogService, times(1)).logCurrentUserAction(
                eq(AuditModule.CONFIG),
                eq(AuditEventType.GLOBAL_CONFIG_UPDATED),
                eq(AuditStatus.SUCCESS),
                eq(AuditSeverity.INFO),
                eq(AuditTargetType.SYSTEM_CONFIG),
                eq(1L),
                isNull(),
                eq("Global configuration updated successfully"),
                anyMap(),
                anyMap()
        );
    }

    @Test
    void updateGlobalConfig_shouldThrowException_whenTaxPercentageIsInvalid() {
        // Arrange
        User superAdmin = buildSuperAdminUser();
        UpdateGlobalConfigRequest request = buildValidGlobalConfigRequest();
        request.setTaxPercentage(new BigDecimal("150.00"));

        setAuthenticatedUser(superAdmin);

        when(userRepository.findById(1L)).thenReturn(Optional.of(superAdmin));

        // Act + Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> systemConfigService.updateGlobalConfig(request)
        );

        assertEquals("Tax percentage must be between 0 and 100", exception.getMessage());

        verifyNoInteractions(systemConfigRepository);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void getBranchConfig_shouldReturnBranchConfig_whenSuperAdminAccessesBranch() {
        // Arrange
        User superAdmin = buildSuperAdminUser();
        Branch branch = buildBranch(2L, "Branch 02");
        BranchConfig config = buildBranchConfig(5L, branch);

        setAuthenticatedUser(superAdmin);

        when(userRepository.findById(1L)).thenReturn(Optional.of(superAdmin));
        when(branchRepository.findById(2L)).thenReturn(Optional.of(branch));
        when(branchConfigRepository.findByBranch(branch)).thenReturn(Optional.of(config));

        // Act
        BranchConfigResponse response = systemConfigService.getBranchConfig(2L);

        // Assert
        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertEquals(2L, response.getBranchId());
        assertEquals("Branch 02", response.getBranchName());

        assertEquals(new BigDecimal("8.50"), response.getDeliveryFee());
        assertTrue(response.isDeliveryEnabled());
        assertTrue(response.isPickupEnabled());
        assertTrue(response.isDineInEnabled());
        assertTrue(response.isBranchActiveForOrders());

        verify(branchRepository, times(1)).findById(2L);
        verify(branchConfigRepository, times(1)).findByBranch(branch);
    }

    @Test
    void updateBranchConfig_shouldUpdateConfigAndWriteAuditLog_whenSuperAdminAccessesBranch() {
        // Arrange
        User superAdmin = buildSuperAdminUser();
        Branch branch = buildBranch(2L, "Branch 02");
        BranchConfig config = buildBranchConfig(5L, branch);

        UpdateBranchConfigRequest request = new UpdateBranchConfigRequest();
        request.setDeliveryFee(new BigDecimal("12.00"));
        request.setDeliveryEnabled(false);
        request.setPickupEnabled(true);
        request.setDineInEnabled(true);
        request.setBranchActiveForOrders(false);

        setAuthenticatedUser(superAdmin);

        when(userRepository.findById(1L)).thenReturn(Optional.of(superAdmin));
        when(branchRepository.findById(2L)).thenReturn(Optional.of(branch));
        when(branchConfigRepository.findByBranch(branch)).thenReturn(Optional.of(config));
        when(branchConfigRepository.save(any(BranchConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BranchConfigResponse response = systemConfigService.updateBranchConfig(2L, request);

        // Assert
        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertEquals(2L, response.getBranchId());

        assertEquals(new BigDecimal("12.00"), response.getDeliveryFee());
        assertFalse(response.isDeliveryEnabled());
        assertTrue(response.isPickupEnabled());
        assertTrue(response.isDineInEnabled());
        assertFalse(response.isBranchActiveForOrders());

        verify(branchConfigRepository, times(1)).save(config);

        verify(auditLogService, times(1)).logCurrentUserAction(
                eq(AuditModule.CONFIG),
                eq(AuditEventType.BRANCH_CONFIG_UPDATED),
                eq(AuditStatus.SUCCESS),
                eq(AuditSeverity.INFO),
                eq(AuditTargetType.BRANCH_CONFIG),
                eq(5L),
                eq(2L),
                eq("Branch configuration updated successfully"),
                anyMap(),
                anyMap()
        );
    }

    @Test
    void updateBranchConfig_shouldThrowException_whenDeliveryFeeIsNegative() {
        // Arrange
        User superAdmin = buildSuperAdminUser();

        UpdateBranchConfigRequest request = new UpdateBranchConfigRequest();
        request.setDeliveryFee(new BigDecimal("-1.00"));
        request.setDeliveryEnabled(true);
        request.setPickupEnabled(true);
        request.setDineInEnabled(true);
        request.setBranchActiveForOrders(true);

        setAuthenticatedUser(superAdmin);

        when(userRepository.findById(1L)).thenReturn(Optional.of(superAdmin));

        // Act + Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> systemConfigService.updateBranchConfig(2L, request)
        );

        assertEquals("Delivery fee cannot be negative", exception.getMessage());

        verify(branchRepository, never()).findById(anyLong());
        verify(branchConfigRepository, never()).save(any(BranchConfig.class));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void getBranchConfig_shouldThrowException_whenAdminTriesToAccessConfig() {
        // Arrange
        Role adminRole = buildRole(2L, "ADMIN");
        User adminUser = buildUser(10L, "admin@test.com", adminRole);

        setAuthenticatedUser(adminUser);

        when(userRepository.findById(10L)).thenReturn(Optional.of(adminUser));

        // Act + Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> systemConfigService.getBranchConfig(2L)
        );

        assertEquals("Only SUPER_ADMIN can access system configuration", exception.getMessage());

        verify(branchRepository, never()).findById(2L);
        verifyNoInteractions(branchConfigRepository);
    }

    @Test
    void getEffectiveBranchConfig_shouldCombineGlobalAndBranchConfig() {
        // Arrange
        User superAdmin = buildSuperAdminUser();
        Branch branch = buildBranch(2L, "Branch 02");

        SystemConfig globalConfig = buildSystemConfig(1L);
        globalConfig.setTaxEnabled(true);
        globalConfig.setTaxPercentage(new BigDecimal("10.00"));
        globalConfig.setServiceChargeEnabled(true);
        globalConfig.setServiceChargePercentage(new BigDecimal("5.00"));
        globalConfig.setLoyaltyEnabled(true);
        globalConfig.setPointsPerAmount(new BigDecimal("1.00"));
        globalConfig.setAmountPerPoint(new BigDecimal("100.00"));
        globalConfig.setMinPointsToRedeem(50);
        globalConfig.setValuePerPoint(new BigDecimal("2.00"));

        BranchConfig branchConfig = buildBranchConfig(5L, branch);

        setAuthenticatedUser(superAdmin);

        when(userRepository.findById(1L)).thenReturn(Optional.of(superAdmin));
        when(branchRepository.findById(2L)).thenReturn(Optional.of(branch));
        when(systemConfigRepository.findAll()).thenReturn(List.of(globalConfig));
        when(branchConfigRepository.findByBranch(branch)).thenReturn(Optional.of(branchConfig));

        // Act
        EffectiveBranchConfigResponse response = systemConfigService.getEffectiveBranchConfig(2L);

        // Assert
        assertNotNull(response);
        assertEquals(2L, response.getBranchId());
        assertEquals("Branch 02", response.getBranchName());

        assertTrue(response.isTaxEnabled());
        assertEquals(new BigDecimal("10.00"), response.getTaxPercentage());

        assertTrue(response.isServiceChargeEnabled());
        assertEquals(new BigDecimal("5.00"), response.getServiceChargePercentage());

        assertTrue(response.isLoyaltyEnabled());
        assertEquals(new BigDecimal("1.00"), response.getPointsPerAmount());
        assertEquals(new BigDecimal("100.00"), response.getAmountPerPoint());
        assertEquals(50, response.getMinPointsToRedeem());
        assertEquals(new BigDecimal("2.00"), response.getValuePerPoint());

        assertEquals(new BigDecimal("8.50"), response.getDeliveryFee());
        assertTrue(response.isDeliveryEnabled());
        assertTrue(response.isPickupEnabled());
        assertTrue(response.isDineInEnabled());
        assertTrue(response.isBranchActiveForOrders());
    }

    /*
     * Put a fake authenticated user into Spring Security context.
     * SystemConfigService reads the current user from SecurityContextHolder.
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

    private UpdateGlobalConfigRequest buildValidGlobalConfigRequest() {
        UpdateGlobalConfigRequest request = new UpdateGlobalConfigRequest();

        request.setTaxEnabled(true);
        request.setTaxPercentage(new BigDecimal("10.00"));
        request.setServiceChargeEnabled(true);
        request.setServiceChargePercentage(new BigDecimal("5.00"));
        request.setLoyaltyEnabled(true);
        request.setPointsPerAmount(new BigDecimal("1.00"));
        request.setAmountPerPoint(new BigDecimal("100.00"));
        request.setMinPointsToRedeem(50);
        request.setValuePerPoint(new BigDecimal("2.00"));
        request.setOrderCancelWindowMinutes(0);

        return request;
    }

    private SystemConfig buildSystemConfig(Long id) {
        SystemConfig config = new SystemConfig();

        config.setId(id);
        config.setTaxEnabled(false);
        config.setTaxPercentage(BigDecimal.ZERO);
        config.setServiceChargeEnabled(false);
        config.setServiceChargePercentage(BigDecimal.ZERO);
        config.setLoyaltyEnabled(false);
        config.setPointsPerAmount(BigDecimal.ZERO);
        config.setAmountPerPoint(BigDecimal.ONE);
        config.setMinPointsToRedeem(0);
        config.setValuePerPoint(BigDecimal.ZERO);
        config.setOrderCancelWindowMinutes(0);

        return config;
    }

    private BranchConfig buildBranchConfig(Long id, Branch branch) {
        BranchConfig config = new BranchConfig();

        config.setId(id);
        config.setBranch(branch);
        config.setDeliveryFee(new BigDecimal("8.50"));
        config.setDeliveryEnabled(true);
        config.setPickupEnabled(true);
        config.setDineInEnabled(true);
        config.setBranchActiveForOrders(true);

        return config;
    }

    private Branch buildBranch(Long id, String name) {
        return Branch.builder()
                .id(id)
                .name(name)
                .address("Test Address")
                .contactNumber("0779999999")
                .email("branch@test.com")
                .status(BranchStatus.ACTIVE)
                .build();
    }

    private Role buildRole(Long id, String name) {
        return Role.builder()
                .id(id)
                .name(name)
                .description(name + " role")
                .build();
    }

    private User buildSuperAdminUser() {
        Role superAdminRole = buildRole(1L, "SUPER_ADMIN");
        return buildUser(1L, "superadmin@test.com", superAdminRole);
    }

    private User buildUser(Long id, String email, Role role) {
        return User.builder()
                .id(id)
                .fullName("Test User")
                .username("testuser")
                .email(email)
                .phone("0771234567")
                .password("encoded-password")
                .role(role)
                .isActive(true)
                .passwordChanged(true)
                .inviteStatus(InviteStatus.SENT)
                .emailSent(true)
                .build();
    }
}