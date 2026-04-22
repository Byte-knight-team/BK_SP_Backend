package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.superadmin.OperatingHourItemRequest;
import com.ByteKnights.com.resturarent_system.dto.request.superadmin.UpdateBranchConfigRequest;
import com.ByteKnights.com.resturarent_system.dto.request.superadmin.UpdateGlobalConfigRequest;
import com.ByteKnights.com.resturarent_system.dto.request.superadmin.UpdateOperatingHoursRequest;
import com.ByteKnights.com.resturarent_system.dto.response.superadmin.BranchConfigResponse;
import com.ByteKnights.com.resturarent_system.dto.response.superadmin.EffectiveBranchConfigResponse;
import com.ByteKnights.com.resturarent_system.dto.response.superadmin.GlobalConfigResponse;
import com.ByteKnights.com.resturarent_system.dto.response.superadmin.OperatingHourItemResponse;
import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.entity.BranchConfig;
import com.ByteKnights.com.resturarent_system.entity.BranchOperatingHours;
import com.ByteKnights.com.resturarent_system.entity.SystemConfig;
import com.ByteKnights.com.resturarent_system.repository.BranchConfigRepository;
import com.ByteKnights.com.resturarent_system.repository.BranchOperatingHoursRepository;
import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import com.ByteKnights.com.resturarent_system.repository.SystemConfigRepository;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;
    private final BranchRepository branchRepository;
    private final BranchConfigRepository branchConfigRepository;
    private final BranchOperatingHoursRepository branchOperatingHoursRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;

    public SystemConfigService(SystemConfigRepository systemConfigRepository,
            BranchRepository branchRepository,
            BranchConfigRepository branchConfigRepository,
            BranchOperatingHoursRepository branchOperatingHoursRepository,
            UserRepository userRepository,
            StaffRepository staffRepository) {
        this.systemConfigRepository = systemConfigRepository;
        this.branchRepository = branchRepository;
        this.branchConfigRepository = branchConfigRepository;
        this.branchOperatingHoursRepository = branchOperatingHoursRepository;
        this.userRepository = userRepository;
        this.staffRepository = staffRepository;
    }

    public GlobalConfigResponse getGlobalConfig() {
        SystemConfig config = getOrCreateGlobalConfig();
        return mapGlobalConfig(config);
    }

    public GlobalConfigResponse updateGlobalConfig(UpdateGlobalConfigRequest request) {
        validateGlobalConfigRequest(request);

        SystemConfig config = getOrCreateGlobalConfig();

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
        return mapGlobalConfig(saved);
    }

    public BranchConfigResponse getBranchConfig(Long branchId) {
        validateBranchAccess(branchId);
    
        Branch branch = getBranchOrThrow(branchId);
        BranchConfig config = getOrCreateBranchConfig(branch);
        return mapBranchConfig(config);
    }

    public BranchConfigResponse updateBranchConfig(Long branchId, UpdateBranchConfigRequest request) {
        validateAdminBranchWriteAccess(branchId);
        validateBranchConfigRequest(request);
    
        Branch branch = getBranchOrThrow(branchId);
        BranchConfig config = getOrCreateBranchConfig(branch);
    
        config.setDeliveryFee(request.getDeliveryFee());
        config.setDeliveryEnabled(request.getDeliveryEnabled());
        config.setPickupEnabled(request.getPickupEnabled());
        config.setDineInEnabled(request.getDineInEnabled());
        config.setBranchActiveForOrders(request.getBranchActiveForOrders());
    
        BranchConfig saved = branchConfigRepository.save(config);
        return mapBranchConfig(saved);
    }

    public List<OperatingHourItemResponse> getOperatingHours(Long branchId) {
        validateBranchAccess(branchId);
    
        Branch branch = getBranchOrThrow(branchId);
    
        List<BranchOperatingHours> hours = branchOperatingHoursRepository.findByBranch(branch);
        return hours.stream()
                .sorted(Comparator.comparing(item -> item.getDayOfWeek().getValue()))
                .map(this::mapOperatingHour)
                .collect(Collectors.toList());
    }

    public List<OperatingHourItemResponse> updateOperatingHours(Long branchId, UpdateOperatingHoursRequest request) {
        validateAdminBranchWriteAccess(branchId);
        validateOperatingHoursRequest(request);

        Branch branch = getBranchOrThrow(branchId);

        branchOperatingHoursRepository.deleteByBranch(branch);

        List<BranchOperatingHours> newRows = new ArrayList<>();
        for (OperatingHourItemRequest item : request.getOperatingHours()) {
            BranchOperatingHours entity = new BranchOperatingHours();
            entity.setBranch(branch);
            entity.setDayOfWeek(parseDayOfWeek(item.getDayOfWeek()));
            entity.setOpen(Boolean.TRUE.equals(item.getIsOpen()));

            if (Boolean.TRUE.equals(item.getIsOpen())) {
                LocalTime openTime = parseTime(item.getOpenTime(), "openTime");
                LocalTime closeTime = parseTime(item.getCloseTime(), "closeTime");

                if (!closeTime.isAfter(openTime)) {
                    throw new IllegalArgumentException("closeTime must be after openTime for " + item.getDayOfWeek());
                }

                entity.setOpenTime(openTime);
                entity.setCloseTime(closeTime);

                if (item.getLastOrderTime() != null && !item.getLastOrderTime().isBlank()) {
                    LocalTime lastOrderTime = parseTime(item.getLastOrderTime(), "lastOrderTime");

                    if (lastOrderTime.isBefore(openTime) || lastOrderTime.isAfter(closeTime)) {
                        throw new IllegalArgumentException("lastOrderTime must be between openTime and closeTime for " + item.getDayOfWeek());
                    }

                    entity.setLastOrderTime(lastOrderTime);
                }
            } else {
                entity.setOpenTime(null);
                entity.setCloseTime(null);
                entity.setLastOrderTime(null);
            }

            newRows.add(entity);
        }

        List<BranchOperatingHours> saved = branchOperatingHoursRepository.saveAll(newRows);

        return saved.stream()
                .sorted(Comparator.comparing(item -> item.getDayOfWeek().getValue()))
                .map(this::mapOperatingHour)
                .collect(Collectors.toList());
    }

    public EffectiveBranchConfigResponse getEffectiveBranchConfig(Long branchId) {
        validateBranchAccess(branchId);
    
        Branch branch = getBranchOrThrow(branchId);
        SystemConfig global = getOrCreateGlobalConfig();
        BranchConfig branchConfig = getOrCreateBranchConfig(branch);
    
        List<OperatingHourItemResponse> operatingHours = branchOperatingHoursRepository.findByBranch(branch)
                .stream()
                .sorted(Comparator.comparing(item -> item.getDayOfWeek().getValue()))
                .map(this::mapOperatingHour)
                .collect(Collectors.toList());
    
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
    
        response.setOperatingHours(operatingHours);
    
        return response;
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

    private SystemConfig getOrCreateGlobalConfig() {
        return systemConfigRepository.findAll().stream().findFirst().orElseGet(() -> {
            SystemConfig config = new SystemConfig();
            return systemConfigRepository.save(config);
        });
    }

    private BranchConfig getOrCreateBranchConfig(Branch branch) {
        return branchConfigRepository.findByBranch(branch).orElseGet(() -> {
            BranchConfig config = new BranchConfig();
            config.setBranch(branch);
            return branchConfigRepository.save(config);
        });
    }

    private Branch getBranchOrThrow(Long branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + branchId));
    }

    private void validateGlobalConfigRequest(UpdateGlobalConfigRequest request) {
        if (request.getTaxPercentage().doubleValue() < 0 || request.getTaxPercentage().doubleValue() > 100) {
            throw new IllegalArgumentException("Tax percentage must be between 0 and 100");
        }

        if (request.getServiceChargePercentage().doubleValue() < 0 || request.getServiceChargePercentage().doubleValue() > 100) {
            throw new IllegalArgumentException("Service charge percentage must be between 0 and 100");
        }

        if (request.getAmountPerPoint().doubleValue() <= 0) {
            throw new IllegalArgumentException("Amount per point must be greater than 0");
        }
    }

    private void validateBranchConfigRequest(UpdateBranchConfigRequest request) {
        if (request.getDeliveryFee().doubleValue() < 0) {
            throw new IllegalArgumentException("Delivery fee cannot be negative");
        }
    }

    private void validateOperatingHoursRequest(UpdateOperatingHoursRequest request) {
        if (request.getOperatingHours() == null || request.getOperatingHours().isEmpty()) {
            throw new IllegalArgumentException("Operating hours list cannot be empty");
        }

        Set<DayOfWeek> uniqueDays = new HashSet<>();
        for (OperatingHourItemRequest item : request.getOperatingHours()) {
            DayOfWeek day = parseDayOfWeek(item.getDayOfWeek());
            if (!uniqueDays.add(day)) {
                throw new IllegalArgumentException("Duplicate dayOfWeek found: " + item.getDayOfWeek());
            }

            boolean isOpen = Boolean.TRUE.equals(item.getIsOpen());

            if (isOpen) {
                if (item.getOpenTime() == null || item.getOpenTime().isBlank()) {
                    throw new IllegalArgumentException("openTime is required when isOpen = true for " + item.getDayOfWeek());
                }
                if (item.getCloseTime() == null || item.getCloseTime().isBlank()) {
                    throw new IllegalArgumentException("closeTime is required when isOpen = true for " + item.getDayOfWeek());
                }
            }
        }
    }

    /**
     * TODO:
     * Connect your existing branch-scoped ADMIN logic here.
     *
     * Right now this method is intentionally empty because I cannot safely guess
     * your exact User / Staff / current-auth repository methods from here.
     *
     * What you should do:
     * - if SUPER_ADMIN -> allow
     * - if ADMIN -> allow only when branchId == admin's own branch
     * - otherwise deny
     */
    private void validateAdminBranchWriteAccess(Long branchId) {
        validateBranchAccess(branchId);
    }

    private void validateBranchAccess(Long branchId) {
        User creator = getCurrentAuthenticatedUser();
        String creatorRole = normalize(creator.getRole().getName());
    
        if ("SUPER_ADMIN".equals(creatorRole)) {
            return;
        }
    
        if ("ADMIN".equals(creatorRole)) {
            Staff creatorStaff = staffRepository.findByUserId(creator.getId())
                    .orElseThrow(() -> new RuntimeException("Admin staff profile not found"));
    
            Long adminBranchId = creatorStaff.getBranch().getId();
    
            if (!adminBranchId.equals(branchId)) {
                throw new RuntimeException("ADMIN can access configuration only for their own branch");
            }
    
            return;
        }
    
        throw new RuntimeException("Only SUPER_ADMIN or ADMIN can access branch configuration");
    }

    private DayOfWeek parseDayOfWeek(String value) {
        try {
            return DayOfWeek.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid dayOfWeek: " + value);
        }
    }

    private LocalTime parseTime(String value, String fieldName) {
        try {
            return LocalTime.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid time format for " + fieldName + ". Use HH:mm:ss or HH:mm");
        }
    }

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

    private OperatingHourItemResponse mapOperatingHour(BranchOperatingHours entity) {
        OperatingHourItemResponse response = new OperatingHourItemResponse();
        response.setId(entity.getId());
        response.setDayOfWeek(entity.getDayOfWeek().name());
        response.setOpen(entity.isOpen());
        response.setOpenTime(entity.getOpenTime() != null ? entity.getOpenTime().toString() : null);
        response.setCloseTime(entity.getCloseTime() != null ? entity.getCloseTime().toString() : null);
        response.setLastOrderTime(entity.getLastOrderTime() != null ? entity.getLastOrderTime().toString() : null);
        return response;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toUpperCase();
    }
}