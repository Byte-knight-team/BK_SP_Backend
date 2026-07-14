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
import com.ByteKnights.com.resturarent_system.entity.BranchStatus;
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

    /*
     * Returns the single global system configuration.
     */
    public GlobalConfigResponse getGlobalConfig() {
        validateSuperAdminAccess();

        SystemConfig config = getOrCreateGlobalConfig();

        return mapGlobalConfig(config);
    }

    /*
     * Updates global system settings.
     *
     * Manual audit is used because old and new configuration values
     * are important for governance.
     */
    public GlobalConfigResponse updateGlobalConfig(
            UpdateGlobalConfigRequest request
    ) {
        validateSuperAdminAccess();
        validateGlobalConfigRequest(request);

        Branch deliveryBranch = getValidDeliveryBranch(
                request.getDeliveryBranchId()
        );

        SystemConfig config = getOrCreateGlobalConfig();

        Map<String, Object> oldValues =
                buildGlobalConfigSnapshot(config);

        config.setDeliveryBranch(deliveryBranch);

        config.setTaxEnabled(
                request.getTaxEnabled()
        );

        config.setTaxPercentage(
                request.getTaxPercentage()
        );

        config.setServiceChargeEnabled(
                request.getServiceChargeEnabled()
        );

        config.setServiceChargePercentage(
                request.getServiceChargePercentage()
        );

        config.setLoyaltyEnabled(
                request.getLoyaltyEnabled()
        );

        config.setPointsPerAmount(
                request.getPointsPerAmount()
        );

        config.setAmountPerPoint(
                request.getAmountPerPoint()
        );

        config.setMinPointsToRedeem(
                request.getMinPointsToRedeem()
        );

        config.setValuePerPoint(
                request.getValuePerPoint()
        );

        config.setOrderCancelWindowMinutes(
                request.getOrderCancelWindowMinutes()
        );

        SystemConfig saved =
                systemConfigRepository.save(config);

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

    /*
     * Returns branch-specific order, delivery, and reservation
     * configuration.
     */
    public BranchConfigResponse getBranchConfig(
            Long branchId
    ) {
        validateSuperAdminAccess();

        Branch branch = getBranchOrThrow(branchId);

        BranchConfig config =
                getOrCreateBranchConfig(branch);

        return mapBranchConfig(config);
    }

    /*
     * Updates branch-specific configuration.
     *
     * Reservation fields are updated only when they are included
     * in the request. This keeps older clients compatible.
     */
    public BranchConfigResponse updateBranchConfig(
            Long branchId,
            UpdateBranchConfigRequest request
    ) {
        validateSuperAdminAccess();
        validateBranchConfigRequest(request);

        Branch branch = getBranchOrThrow(branchId);

        BranchConfig config =
                getOrCreateBranchConfig(branch);

        Map<String, Object> oldValues =
                buildBranchConfigSnapshot(config);

        /*
         * Delivery and order configuration
         */

        config.setDeliveryFee(
                request.getDeliveryFee()
        );

        config.setDeliveryFeePerKm(
                request.getDeliveryFeePerKm()
        );

        config.setMaxDeliveryRadiusKm(
                request.getMaxDeliveryRadiusKm()
        );

        config.setDeliveryEnabled(
                request.getDeliveryEnabled()
        );

        config.setPickupEnabled(
                request.getPickupEnabled()
        );

        config.setDineInEnabled(
                request.getDineInEnabled()
        );

        config.setBranchActiveForOrders(
                request.getBranchActiveForOrders()
        );

        /*
         * Reservation configuration
         *
         * Only update a reservation value when it was included in
         * the request. Missing values preserve the current settings.
         */

        if (request.getReservationFeePerHour() != null) {
            config.setReservationFeePerHour(
                    request.getReservationFeePerHour()
            );
        }

        if (request.getReservationHandlingFee() != null) {
            config.setReservationHandlingFee(
                    request.getReservationHandlingFee()
            );
        }

        if (request.getReservationPaymentWindowMinutes() != null) {
            config.setReservationPaymentWindowMinutes(
                    request.getReservationPaymentWindowMinutes()
            );
        }

        if (request.getReservationMinLeadHours() != null) {
            config.setReservationMinLeadHours(
                    request.getReservationMinLeadHours()
            );
        }

        if (request.getReservationMaxGuestCount() != null) {
            config.setReservationMaxGuestCount(
                    request.getReservationMaxGuestCount()
            );
        }

        if (request.getReservationsEnabled() != null) {
            config.setReservationsEnabled(
                    request.getReservationsEnabled()
            );
        }

        BranchConfig saved =
                branchConfigRepository.save(config);

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
     * Returns the final configuration applied to a branch.
     *
     * It combines:
     * - global configuration
     * - branch-specific order and delivery configuration
     */
    public EffectiveBranchConfigResponse getEffectiveBranchConfig(
            Long branchId
    ) {
        validateSuperAdminAccess();

        Branch branch = getBranchOrThrow(branchId);

        SystemConfig global = getOrCreateGlobalConfig();

        BranchConfig branchConfig =
                getOrCreateBranchConfig(branch);

        EffectiveBranchConfigResponse response =
                new EffectiveBranchConfigResponse();

        response.setBranchId(
                branch.getId()
        );

        response.setBranchName(
                branch.getName()
        );

        response.setTaxEnabled(
                global.isTaxEnabled()
        );

        response.setTaxPercentage(
                global.getTaxPercentage()
        );

        response.setServiceChargeEnabled(
                global.isServiceChargeEnabled()
        );

        response.setServiceChargePercentage(
                global.getServiceChargePercentage()
        );

        response.setLoyaltyEnabled(
                global.isLoyaltyEnabled()
        );

        response.setPointsPerAmount(
                global.getPointsPerAmount()
        );

        response.setAmountPerPoint(
                global.getAmountPerPoint()
        );

        response.setMinPointsToRedeem(
                global.getMinPointsToRedeem()
        );

        response.setValuePerPoint(
                global.getValuePerPoint()
        );

        response.setOrderCancelWindowMinutes(
                global.getOrderCancelWindowMinutes()
        );

        response.setDeliveryFee(
                branchConfig.getDeliveryFee()
        );

        response.setDeliveryFeePerKm(
                branchConfig.getDeliveryFeePerKm()
        );

        response.setMaxDeliveryRadiusKm(
                branchConfig.getMaxDeliveryRadiusKm()
        );

        response.setDeliveryEnabled(
                branchConfig.isDeliveryEnabled()
        );

        response.setPickupEnabled(
                branchConfig.isPickupEnabled()
        );

        response.setDineInEnabled(
                branchConfig.isDineInEnabled()
        );

        response.setBranchActiveForOrders(
                branchConfig.isBranchActiveForOrders()
        );

        return response;
    }

    /*
     * Returns the currently authenticated user.
     */
    private User getCurrentAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null) {
            throw new RuntimeException(
                    "Authenticated user not found"
            );
        }

        Object principal =
                authentication.getPrincipal();

        if (principal instanceof JwtUserPrincipal jwtUser) {
            return userRepository.findByEmail(
                            jwtUser.getEmail()
                    )
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Authenticated user not found"
                            )
                    );
        }

        if (principal instanceof User user) {
            return userRepository.findById(
                            user.getId()
                    )
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Authenticated user not found"
                            )
                    );
        }

        String email = authentication.getName();

        if (
                email != null &&
                !email.trim().isEmpty()
        ) {
            return userRepository.findByEmail(email)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Authenticated user not found"
                            )
                    );
        }

        throw new RuntimeException(
                "Authenticated user not found"
        );
    }

    /*
     * Extra service-layer access protection.
     */
    private void validateSuperAdminAccess() {
        User user = getCurrentAuthenticatedUser();

        String roleName = normalize(
                user.getRole().getName()
        );

        if (!"SUPER_ADMIN".equals(roleName)) {
            throw new RuntimeException(
                    "Only SUPER_ADMIN can access system configuration"
            );
        }
    }

    /*
     * Returns the existing global configuration or creates
     * a default one.
     */
    private SystemConfig getOrCreateGlobalConfig() {
        return systemConfigRepository
                .findFirstByOrderByIdAsc()
                .orElseGet(() ->
                        systemConfigRepository.save(
                                new SystemConfig()
                        )
                );
    }

    /*
     * Returns the existing branch configuration or creates
     * default configuration for a newly created branch.
     */
    private BranchConfig getOrCreateBranchConfig(
            Branch branch
    ) {
        return branchConfigRepository
                .findByBranch(branch)
                .orElseGet(() -> {
                    BranchConfig config =
                            new BranchConfig();

                    config.setBranch(branch);

                    return branchConfigRepository.save(
                            config
                    );
                });
    }

    /*
     * Loads one branch or throws an error.
     */
    private Branch getBranchOrThrow(
            Long branchId
    ) {
        return branchRepository.findById(branchId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Branch not found with id: " +
                                        branchId
                        )
                );
    }

    /*
     * Loads and validates the branch selected as the
     * system delivery branch.
     */
    private Branch getValidDeliveryBranch(
            Long branchId
    ) {
        if (branchId == null) {
            throw new IllegalArgumentException(
                    "Delivery branch must be selected"
            );
        }

        Branch branch =
                branchRepository.findById(branchId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Selected delivery branch was not found"
                                )
                        );

        if (branch.getStatus() != BranchStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Only an active branch can be selected as the delivery branch"
            );
        }

        if (
                branch.getLatitude() == null ||
                branch.getLongitude() == null
        ) {
            throw new IllegalArgumentException(
                    "The selected delivery branch does not have a map location"
            );
        }

        return branch;
    }

    /*
     * Validates global configuration values.
     */
    private void validateGlobalConfigRequest(
            UpdateGlobalConfigRequest request
    ) {
        if (
                request.getTaxPercentage()
                        .doubleValue() < 0 ||
                request.getTaxPercentage()
                        .doubleValue() > 100
        ) {
            throw new IllegalArgumentException(
                    "Tax percentage must be between 0 and 100"
            );
        }

        if (
                request.getServiceChargePercentage()
                        .doubleValue() < 0 ||
                request.getServiceChargePercentage()
                        .doubleValue() > 100
        ) {
            throw new IllegalArgumentException(
                    "Service charge percentage must be between 0 and 100"
            );
        }

        if (
                request.getAmountPerPoint()
                        .doubleValue() <= 0
        ) {
            throw new IllegalArgumentException(
                    "Amount per point must be greater than 0"
            );
        }
    }

    /*
     * Validates branch delivery, order, and reservation
     * configuration.
     */
    private void validateBranchConfigRequest(
            UpdateBranchConfigRequest request
    ) {
        if (
                request.getDeliveryFee()
                        .doubleValue() < 0
        ) {
            throw new IllegalArgumentException(
                    "Delivery fee cannot be negative"
            );
        }

        if (
                request.getDeliveryFeePerKm()
                        .doubleValue() < 0
        ) {
            throw new IllegalArgumentException(
                    "Delivery fee per kilometre cannot be negative"
            );
        }

        if (
                request.getMaxDeliveryRadiusKm() < 0
        ) {
            throw new IllegalArgumentException(
                    "Maximum delivery radius cannot be negative"
            );
        }

        if (
                request.getReservationFeePerHour() != null &&
                request.getReservationFeePerHour()
                        .doubleValue() < 0
        ) {
            throw new IllegalArgumentException(
                    "Reservation fee per hour cannot be negative"
            );
        }

        if (
                request.getReservationHandlingFee() != null &&
                request.getReservationHandlingFee()
                        .doubleValue() < 0
        ) {
            throw new IllegalArgumentException(
                    "Reservation handling fee cannot be negative"
            );
        }

        if (
                request.getReservationPaymentWindowMinutes() != null &&
                request.getReservationPaymentWindowMinutes() < 1
        ) {
            throw new IllegalArgumentException(
                    "Reservation payment window must be at least 1 minute"
            );
        }

        if (
                request.getReservationMinLeadHours() != null &&
                request.getReservationMinLeadHours() < 0
        ) {
            throw new IllegalArgumentException(
                    "Reservation minimum lead time cannot be negative"
            );
        }

        if (
                request.getReservationMaxGuestCount() != null &&
                request.getReservationMaxGuestCount() < 1
        ) {
            throw new IllegalArgumentException(
                    "Reservation maximum guest count must be at least 1"
            );
        }
    }

    /*
     * Maps global configuration into its API response.
     */
    private GlobalConfigResponse mapGlobalConfig(
            SystemConfig config
    ) {
        GlobalConfigResponse response =
                new GlobalConfigResponse();

        response.setId(
                config.getId()
        );

        Branch deliveryBranch =
                config.getDeliveryBranch();

        response.setDeliveryBranchId(
                deliveryBranch != null
                        ? deliveryBranch.getId()
                        : null
        );

        response.setDeliveryBranchName(
                deliveryBranch != null
                        ? deliveryBranch.getName()
                        : null
        );

        response.setTaxEnabled(
                config.isTaxEnabled()
        );

        response.setTaxPercentage(
                config.getTaxPercentage()
        );

        response.setServiceChargeEnabled(
                config.isServiceChargeEnabled()
        );

        response.setServiceChargePercentage(
                config.getServiceChargePercentage()
        );

        response.setLoyaltyEnabled(
                config.isLoyaltyEnabled()
        );

        response.setPointsPerAmount(
                config.getPointsPerAmount()
        );

        response.setAmountPerPoint(
                config.getAmountPerPoint()
        );

        response.setMinPointsToRedeem(
                config.getMinPointsToRedeem()
        );

        response.setValuePerPoint(
                config.getValuePerPoint()
        );

        response.setOrderCancelWindowMinutes(
                config.getOrderCancelWindowMinutes()
        );

        response.setUpdatedAt(
                config.getUpdatedAt()
        );

        return response;
    }

    /*
     * Maps branch configuration into its API response.
     */
    private BranchConfigResponse mapBranchConfig(
            BranchConfig config
    ) {
        BranchConfigResponse response =
                new BranchConfigResponse();

        response.setId(
                config.getId()
        );

        response.setBranchId(
                config.getBranch().getId()
        );

        response.setBranchName(
                config.getBranch().getName()
        );

        /*
         * Delivery and order configuration
         */

        response.setDeliveryFee(
                config.getDeliveryFee()
        );

        response.setDeliveryFeePerKm(
                config.getDeliveryFeePerKm()
        );

        response.setMaxDeliveryRadiusKm(
                config.getMaxDeliveryRadiusKm()
        );

        response.setDeliveryEnabled(
                config.isDeliveryEnabled()
        );

        response.setPickupEnabled(
                config.isPickupEnabled()
        );

        response.setDineInEnabled(
                config.isDineInEnabled()
        );

        response.setBranchActiveForOrders(
                config.isBranchActiveForOrders()
        );

        /*
         * Reservation configuration
         */

        response.setReservationFeePerHour(
                config.getReservationFeePerHour()
        );

        response.setReservationHandlingFee(
                config.getReservationHandlingFee()
        );

        response.setReservationPaymentWindowMinutes(
                config.getReservationPaymentWindowMinutes()
        );

        response.setReservationMinLeadHours(
                config.getReservationMinLeadHours()
        );

        response.setReservationMaxGuestCount(
                config.getReservationMaxGuestCount()
        );

        response.setReservationsEnabled(
                config.isReservationsEnabled()
        );

        response.setUpdatedAt(
                config.getUpdatedAt()
        );

        return response;
    }

    /*
     * Builds old/new global configuration values for auditing.
     */
    private Map<String, Object> buildGlobalConfigSnapshot(
            SystemConfig config
    ) {
        Map<String, Object> snapshot =
                new LinkedHashMap<>();

        snapshot.put(
                "id",
                config.getId()
        );

        Branch deliveryBranch =
                config.getDeliveryBranch();

        snapshot.put(
                "deliveryBranchId",
                deliveryBranch != null
                        ? deliveryBranch.getId()
                        : null
        );

        snapshot.put(
                "deliveryBranchName",
                deliveryBranch != null
                        ? deliveryBranch.getName()
                        : null
        );

        snapshot.put(
                "taxEnabled",
                config.isTaxEnabled()
        );

        snapshot.put(
                "taxPercentage",
                config.getTaxPercentage()
        );

        snapshot.put(
                "serviceChargeEnabled",
                config.isServiceChargeEnabled()
        );

        snapshot.put(
                "serviceChargePercentage",
                config.getServiceChargePercentage()
        );

        snapshot.put(
                "loyaltyEnabled",
                config.isLoyaltyEnabled()
        );

        snapshot.put(
                "pointsPerAmount",
                config.getPointsPerAmount()
        );

        snapshot.put(
                "amountPerPoint",
                config.getAmountPerPoint()
        );

        snapshot.put(
                "minPointsToRedeem",
                config.getMinPointsToRedeem()
        );

        snapshot.put(
                "valuePerPoint",
                config.getValuePerPoint()
        );

        snapshot.put(
                "orderCancelWindowMinutes",
                config.getOrderCancelWindowMinutes()
        );

        snapshot.put(
                "updatedAt",
                config.getUpdatedAt()
        );

        return snapshot;
    }

    /*
     * Builds old/new branch configuration values for auditing.
     */
    private Map<String, Object> buildBranchConfigSnapshot(
            BranchConfig config
    ) {
        Map<String, Object> snapshot =
                new LinkedHashMap<>();

        snapshot.put(
                "id",
                config.getId()
        );

        snapshot.put(
                "branchId",
                config.getBranch() != null
                        ? config.getBranch().getId()
                        : null
        );

        snapshot.put(
                "branchName",
                config.getBranch() != null
                        ? config.getBranch().getName()
                        : null
        );

        /*
         * Delivery and order configuration
         */

        snapshot.put(
                "deliveryFee",
                config.getDeliveryFee()
        );

        snapshot.put(
                "deliveryFeePerKm",
                config.getDeliveryFeePerKm()
        );

        snapshot.put(
                "maxDeliveryRadiusKm",
                config.getMaxDeliveryRadiusKm()
        );

        snapshot.put(
                "deliveryEnabled",
                config.isDeliveryEnabled()
        );

        snapshot.put(
                "pickupEnabled",
                config.isPickupEnabled()
        );

        snapshot.put(
                "dineInEnabled",
                config.isDineInEnabled()
        );

        snapshot.put(
                "branchActiveForOrders",
                config.isBranchActiveForOrders()
        );

        /*
         * Reservation configuration
         */

        snapshot.put(
                "reservationFeePerHour",
                config.getReservationFeePerHour()
        );

        snapshot.put(
                "reservationHandlingFee",
                config.getReservationHandlingFee()
        );

        snapshot.put(
                "reservationPaymentWindowMinutes",
                config.getReservationPaymentWindowMinutes()
        );

        snapshot.put(
                "reservationMinLeadHours",
                config.getReservationMinLeadHours()
        );

        snapshot.put(
                "reservationMaxGuestCount",
                config.getReservationMaxGuestCount()
        );

        snapshot.put(
                "reservationsEnabled",
                config.isReservationsEnabled()
        );

        snapshot.put(
                "updatedAt",
                config.getUpdatedAt()
        );

        return snapshot;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        return value
                .trim()
                .toUpperCase();
    }
}