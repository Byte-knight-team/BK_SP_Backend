package com.ByteKnights.com.resturarent_system.service;

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
import com.ByteKnights.com.resturarent_system.entity.SystemConfig;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.repository.BranchConfigRepository;
import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import com.ByteKnights.com.resturarent_system.repository.SystemConfigRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;
    private final BranchRepository branchRepository;
    private final BranchConfigRepository branchConfigRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public SystemConfigService(
            SystemConfigRepository systemConfigRepository,
            BranchRepository branchRepository,
            BranchConfigRepository branchConfigRepository,
            UserRepository userRepository,
            AuditLogService auditLogService
    ) {
        this.systemConfigRepository = systemConfigRepository;
        this.branchRepository = branchRepository;
        this.branchConfigRepository = branchConfigRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public GlobalConfigResponse getGlobalConfig() {
        validateSuperAdminAccess();

        SystemConfig config = getOrCreateGlobalConfig();
        return mapGlobalConfig(config);
    }

    /*
     * Updates global system settings.
     * Manual audit is used because old/new config values are important.
     */
    public GlobalConfigResponse updateGlobalConfig(UpdateGlobalConfigRequest request) {
        validateSuperAdminAccess();
        validateGlobalConfigRequest(request);

        SystemConfig config = getOrCreateGlobalConfig();
        Map<String, Object> oldValues = buildGlobalConfigSnapshot(config);

        config.setTaxEnabled(request.getTaxEnabled());
        config.setTaxPercentage(request.getTaxPercentage());
        config.setServiceChargeEnabled(request.getServiceChargeEnabled());
        config.setServiceChargePercentage(request.getServiceChargePercentage());
        config.setLoyaltyEnabled(request.getLoyaltyEnabled());
        config.setPointsPerAmount(request.getPointsPerAmount());
        config.setAmountPerPoint(request.getAmountPerPoint());
        config.setMinPointsToRedeem(request.getMinPointsToRedeem());
        config.setValuePerPoint(request.getValuePerPoint());
        config.setOrderCancelWindowMinutes(request.getOrderCancelWindowMinutes());

        SystemConfig saved = systemConfigRepository.save(config);

        // log the audit trail
        auditLogService.logCurrentUserAction(
                AuditModule.CONFIG,
                AuditEventType.GLOBAL_CONFIG_UPDATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.SYSTEM_CONFIG,
                saved.getId(),
                null,
                "Global configuration updated successfully",
                oldValues,
                buildGlobalConfigSnapshot(saved)
        );

        return mapGlobalConfig(saved);
    }

    //get branch config
    public BranchConfigResponse getBranchConfig(Long branchId) {

        validateSuperAdminAccess();

        Branch branch = getBranchOrThrow(branchId);
        BranchConfig config = getOrCreateBranchConfig(branch);

        return mapBranchConfig(config);
    }

    /*
     * Updates branch specific order settings.
     * Manual audit is used because old/new branch config values are important.
     */
    public BranchConfigResponse updateBranchConfig(Long branchId, UpdateBranchConfigRequest request) {
        
        validateSuperAdminAccess();
        validateBranchConfigRequest(request);

        Branch branch = getBranchOrThrow(branchId);
        BranchConfig config = getOrCreateBranchConfig(branch);
        Map<String, Object> oldValues = buildBranchConfigSnapshot(config);

        config.setDeliveryFee(request.getDeliveryFee());
        config.setDeliveryEnabled(request.getDeliveryEnabled());
        config.setPickupEnabled(request.getPickupEnabled());
        config.setDineInEnabled(request.getDineInEnabled());
        config.setBranchActiveForOrders(request.getBranchActiveForOrders());

        BranchConfig saved = branchConfigRepository.save(config);

        auditLogService.logCurrentUserAction(
                AuditModule.CONFIG,
                AuditEventType.BRANCH_CONFIG_UPDATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.BRANCH_CONFIG,
                saved.getId(),
                branch.getId(),
                "Branch configuration updated successfully",
                oldValues,
                buildBranchConfigSnapshot(saved)
        );

        return mapBranchConfig(saved);
    }

    /*
     * Returns the final config applied to a branch.
     * This now combines only global config + branch config.
     */
    public EffectiveBranchConfigResponse getEffectiveBranchConfig(Long branchId) {
        validateSuperAdminAccess();

        Branch branch = getBranchOrThrow(branchId);
        SystemConfig global = getOrCreateGlobalConfig();
        BranchConfig branchConfig = getOrCreateBranchConfig(branch);

        EffectiveBranchConfigResponse response = new EffectiveBranchConfigResponse();

        response.setBranchId(branch.getId());
        response.setBranchName(branch.getName());

        response.setTaxEnabled(global.isTaxEnabled());
        response.setTaxPercentage(global.getTaxPercentage());

        response.setServiceChargeEnabled(global.isServiceChargeEnabled());
        response.setServiceChargePercentage(global.getServiceChargePercentage());

        response.setLoyaltyEnabled(global.isLoyaltyEnabled());
        response.setPointsPerAmount(global.getPointsPerAmount());
        response.setAmountPerPoint(global.getAmountPerPoint());
        response.setMinPointsToRedeem(global.getMinPointsToRedeem());
        response.setValuePerPoint(global.getValuePerPoint());
        response.setOrderCancelWindowMinutes(global.getOrderCancelWindowMinutes());

        response.setDeliveryFee(branchConfig.getDeliveryFee());
        response.setDeliveryEnabled(branchConfig.isDeliveryEnabled());

        response.setPickupEnabled(branchConfig.isPickupEnabled());
        response.setDineInEnabled(branchConfig.isDineInEnabled());
        response.setBranchActiveForOrders(branchConfig.isBranchActiveForOrders());

        return response;
    }

    //this method is used to get the current authenticated user
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

    // Extra service layer safety.
    // Controller already allows only SUPER_ADMIN, but service also checks it.
    private void validateSuperAdminAccess() {
        User user = getCurrentAuthenticatedUser();
        String roleName = normalize(user.getRole().getName());

        if (!"SUPER_ADMIN".equals(roleName)) {
            throw new RuntimeException("Only SUPER_ADMIN can access system configuration");
        }
    }

    
    //Returns existing global config or creates a default config.
    //First means for all branches this global config will be applied 
    private SystemConfig getOrCreateGlobalConfig() {
        return systemConfigRepository.findAll()
                .stream()
                .findFirst()
                .orElseGet(() -> systemConfigRepository.save(new SystemConfig()));
    }

    /*
     * Returns existing branch config or creates a default config for the branch.
     * Used for a new branch ,not need to manually create branch config for the new branch
     */
    private BranchConfig getOrCreateBranchConfig(Branch branch) {
        return branchConfigRepository.findByBranch(branch).orElseGet(() -> {
            BranchConfig config = new BranchConfig();
            config.setBranch(branch);
            return branchConfigRepository.save(config);
        });
    }

    //get or create branch config and return branch config
    private Branch getBranchOrThrow(Long branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + branchId));
    }

    /*
     * Validates global config values before saving.
     */
    private void validateGlobalConfigRequest(UpdateGlobalConfigRequest request) {

        //validate tax percentage
        if (request.getTaxPercentage().doubleValue() < 0
                || request.getTaxPercentage().doubleValue() > 100) {
            throw new IllegalArgumentException("Tax percentage must be between 0 and 100");
        }

        //validate service charge percentage
        if (request.getServiceChargePercentage().doubleValue() < 0
                || request.getServiceChargePercentage().doubleValue() > 100) {
            throw new IllegalArgumentException("Service charge percentage must be between 0 and 100");
        }

        //validate amount per point
        if (request.getAmountPerPoint().doubleValue() <= 0) {
            throw new IllegalArgumentException("Amount per point must be greater than 0");
        }
    }

    //validate branch config values before saving
    private void validateBranchConfigRequest(UpdateBranchConfigRequest request) {
        //validate delivery fee
        if (request.getDeliveryFee().doubleValue() < 0) {
            throw new IllegalArgumentException("Delivery fee cannot be negative");
        }
    }

    //map global config to global config response
    private GlobalConfigResponse mapGlobalConfig(SystemConfig config) {
        GlobalConfigResponse response = new GlobalConfigResponse();

        response.setId(config.getId());
        response.setTaxEnabled(config.isTaxEnabled());
        response.setTaxPercentage(config.getTaxPercentage());
        response.setServiceChargeEnabled(config.isServiceChargeEnabled());
        response.setServiceChargePercentage(config.getServiceChargePercentage());
        response.setLoyaltyEnabled(config.isLoyaltyEnabled());
        response.setPointsPerAmount(config.getPointsPerAmount());
        response.setAmountPerPoint(config.getAmountPerPoint());
        response.setMinPointsToRedeem(config.getMinPointsToRedeem());
        response.setValuePerPoint(config.getValuePerPoint());
        response.setOrderCancelWindowMinutes(config.getOrderCancelWindowMinutes());
        response.setUpdatedAt(config.getUpdatedAt());

        return response;
    }

    //map branch config to branch config response
    private BranchConfigResponse mapBranchConfig(BranchConfig config) {
        BranchConfigResponse response = new BranchConfigResponse();

        response.setId(config.getId());
        response.setBranchId(config.getBranch().getId());
        response.setBranchName(config.getBranch().getName());
        response.setDeliveryFee(config.getDeliveryFee());
        response.setDeliveryEnabled(config.isDeliveryEnabled());
        response.setPickupEnabled(config.isPickupEnabled());
        response.setDineInEnabled(config.isDineInEnabled());
        response.setBranchActiveForOrders(config.isBranchActiveForOrders());
        response.setUpdatedAt(config.getUpdatedAt());

        return response;
    }

    /*
     * Builds old/new global config data for audit logging.
     */
    private Map<String, Object> buildGlobalConfigSnapshot(SystemConfig config) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        snapshot.put("id", config.getId());
        snapshot.put("taxEnabled", config.isTaxEnabled());
        snapshot.put("taxPercentage", config.getTaxPercentage());
        snapshot.put("serviceChargeEnabled", config.isServiceChargeEnabled());
        snapshot.put("serviceChargePercentage", config.getServiceChargePercentage());
        snapshot.put("loyaltyEnabled", config.isLoyaltyEnabled());
        snapshot.put("pointsPerAmount", config.getPointsPerAmount());
        snapshot.put("amountPerPoint", config.getAmountPerPoint());
        snapshot.put("minPointsToRedeem", config.getMinPointsToRedeem());
        snapshot.put("valuePerPoint", config.getValuePerPoint());
        snapshot.put("orderCancelWindowMinutes", config.getOrderCancelWindowMinutes());
        snapshot.put("updatedAt", config.getUpdatedAt());

        return snapshot;
    }

    /*
     * Builds old/new branch config data for audit logging.
     */
    private Map<String, Object> buildBranchConfigSnapshot(BranchConfig config) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        snapshot.put("id", config.getId());
        snapshot.put("branchId", config.getBranch() != null ? config.getBranch().getId() : null);
        snapshot.put("branchName", config.getBranch() != null ? config.getBranch().getName() : null);
        snapshot.put("deliveryFee", config.getDeliveryFee());
        snapshot.put("deliveryEnabled", config.isDeliveryEnabled());
        snapshot.put("pickupEnabled", config.isPickupEnabled());
        snapshot.put("dineInEnabled", config.isDineInEnabled());
        snapshot.put("branchActiveForOrders", config.isBranchActiveForOrders());
        snapshot.put("updatedAt", config.getUpdatedAt());

        return snapshot;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        return value.trim().toUpperCase();
    }
}