package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.admin.CreateTableRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.UpdateTableRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.UpdateTableStatusRequest;
import com.ByteKnights.com.resturarent_system.dto.response.admin.TableResponse;
import com.ByteKnights.com.resturarent_system.entity.AuditEventType;
import com.ByteKnights.com.resturarent_system.entity.AuditModule;
import com.ByteKnights.com.resturarent_system.entity.AuditSeverity;
import com.ByteKnights.com.resturarent_system.entity.AuditStatus;
import com.ByteKnights.com.resturarent_system.entity.AuditTargetType;
import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.entity.RestaurantTable;
import com.ByteKnights.com.resturarent_system.entity.TableStatus;
import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import com.ByteKnights.com.resturarent_system.repository.RestaurantTableRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.RestaurantTableService;
import com.ByteKnights.com.resturarent_system.exception.ResourceNotFoundException;
import com.ByteKnights.com.resturarent_system.exception.DuplicateResourceException;
import com.ByteKnights.com.resturarent_system.exception.InvalidOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implements table management business rules.
 */
@Service
@RequiredArgsConstructor
public class RestaurantTableServiceImpl implements RestaurantTableService {

    private final RestaurantTableRepository tableRepository;
    private final BranchRepository branchRepository;
    private final StaffRepository staffRepository;
    private final AuditLogService auditLogService;

    /**
     * Creates a table after validating branch state and uniqueness.
     */
    @Override
    @Transactional
    public TableResponse createTable(CreateTableRequest request) {
        enforceAdminBranchAccess(request.getBranchId());

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Branch not found with id: " + request.getBranchId()));

        if (branch.getStatus() != com.ByteKnights.com.resturarent_system.entity.BranchStatus.ACTIVE) {
            throw new InvalidOperationException("Cannot create table in an inactive branch: " + branch.getName());
        }

        if (tableRepository.existsByBranchIdAndTableNumber(
                request.getBranchId(), request.getTableNumber())) {
            throw new DuplicateResourceException(
                    "Table number " + request.getTableNumber()
                            + " already exists in branch " + branch.getName());
        }

        TableStatus status = parseStatus(request.getStatus());

        RestaurantTable table = RestaurantTable.builder()
                .branch(branch)
                .tableNumber(request.getTableNumber())
                .capacity(request.getCapacity())
                .state(status)
                .build();

        RestaurantTable saved = tableRepository.save(table);

        auditLogService.logCurrentUserAction(
                AuditModule.TABLE,
                AuditEventType.TABLE_CREATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.TABLE,
                saved.getId(),
                getTableBranchId(saved),
                "Table created successfully",
                null,
                buildTableAuditSnapshot(saved)
        );

        return mapToResponse(saved);
    }

    /**
     * Fetches one table by id.
     */
    @Override
    @Transactional(readOnly = true)
    public TableResponse getTableById(Long id) {
        RestaurantTable table = findTableOrThrow(id);
        enforceAdminBranchAccess(table.getBranch().getId());
        return mapToResponse(table);
    }

    /**
     * Returns all tables.
     */
    @Override
    @Transactional(readOnly = true)
    public List<TableResponse> getAllTables() {
        Long adminBranchId = resolveCurrentAdminBranchIdOrNull();

        List<RestaurantTable> tables = adminBranchId == null
                ? tableRepository.findAll()
                : tableRepository.findByBranchId(adminBranchId);

        return tables
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Updates table number, capacity, and status when provided.
     */
    @Override
    @Transactional
    public TableResponse updateTable(Long id, UpdateTableRequest request) {
        RestaurantTable table = findTableOrThrow(id);
        enforceAdminBranchAccess(table.getBranch().getId());

        Map<String, Object> oldValues = buildTableAuditSnapshot(table);

        if (request.getTableNumber() != null) {
            if (!table.getTableNumber().equals(request.getTableNumber())
                    && tableRepository.existsByBranchIdAndTableNumberAndIdNot(
                    table.getBranch().getId(), request.getTableNumber(), id)) {
                throw new DuplicateResourceException(
                        "Table number " + request.getTableNumber()
                                + " already exists in branch " + table.getBranch().getName());
            }

            table.setTableNumber(request.getTableNumber());
        }

        if (request.getCapacity() != null) {
            table.setCapacity(request.getCapacity());
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            table.setState(parseStatus(request.getStatus()));
        }

        RestaurantTable updated = tableRepository.save(table);

        auditLogService.logCurrentUserAction(
                AuditModule.TABLE,
                AuditEventType.TABLE_UPDATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.TABLE,
                updated.getId(),
                getTableBranchId(updated),
                "Table updated successfully",
                oldValues,
                buildTableAuditSnapshot(updated)
        );

        return mapToResponse(updated);
    }

    /**
     * Updates only the state of a table.
     */
    @Override
    @Transactional
    public TableResponse updateTableStatus(Long id, UpdateTableStatusRequest request) {
        RestaurantTable table = findTableOrThrow(id);
        enforceAdminBranchAccess(table.getBranch().getId());

        Map<String, Object> oldValues = buildTableAuditSnapshot(table);

        Integer activeOrderCount = table.getActiveOrderCount() == null ? 0 : table.getActiveOrderCount();

        table.setState(parseStatus(request.getStatus()));

        if (table.getState() == TableStatus.AVAILABLE && activeOrderCount > 0) {
            table.setState(TableStatus.OCCUPIED);
        } else if (table.getState() == TableStatus.OCCUPIED && activeOrderCount == 0) {
            table.setState(TableStatus.AVAILABLE);
        }

        RestaurantTable updated = tableRepository.save(table);

        auditLogService.logCurrentUserAction(
                AuditModule.TABLE,
                AuditEventType.TABLE_STATUS_UPDATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.TABLE,
                updated.getId(),
                getTableBranchId(updated),
                "Table status updated successfully",
                oldValues,
                buildTableAuditSnapshot(updated)
        );

        return mapToResponse(updated);
    }

    // ─────────────────────────── Helper Methods ───────────────────────────

    /**
     * Finds a table or throws if not present.
     */
    private RestaurantTable findTableOrThrow(Long id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Table not found with id: " + id));
    }

    /**
     * Parses status text to enum and applies default when empty.
     */
    private TableStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return TableStatus.AVAILABLE;
        }

        try {
            return TableStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidOperationException(
                    "Invalid table status: " + status
                            + ". Valid values: AVAILABLE, OCCUPIED, RESERVED");
        }
    }

    /**
     * Maps entity data to API response DTO.
     */
    private TableResponse mapToResponse(RestaurantTable table) {
        return TableResponse.builder()
                .id(table.getId())
                .tableNumber(table.getTableNumber())
                .capacity(table.getCapacity())
                .status(table.getState().name())
                .branchId(table.getBranch().getId())
                .branchName(table.getBranch().getName())
                .currentGuestCount(table.getCurrentGuestCount())
                .activeOrderCount(table.getActiveOrderCount())
                .createdAt(table.getCreatedAt())
                .build();
    }

    private void enforceAdminBranchAccess(Long targetBranchId) {
        Long adminBranchId = resolveCurrentAdminBranchIdOrNull();

        if (adminBranchId != null && !adminBranchId.equals(targetBranchId)) {
            throw new InvalidOperationException("ADMIN can access tables only in their own branch");
        }
    }

    private Long resolveCurrentAdminBranchIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getAuthorities() == null) {
            return null;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!isAdmin) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof JwtUserPrincipal jwtUser)
                || jwtUser.getUser() == null
                || jwtUser.getUser().getId() == null) {
            throw new InvalidOperationException("Authenticated ADMIN user not found");
        }

        return staffRepository.findByUserId(jwtUser.getUser().getId())
                .map(staff -> staff.getBranch().getId())
                .orElseThrow(() -> new InvalidOperationException("Admin staff profile not found"));
    }

    /*
     * Builds safe audit JSON for table actions.
     */
    private Map<String, Object> buildTableAuditSnapshot(RestaurantTable table) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (table == null) {
            return snapshot;
        }

        //This used to get the snapshot before updating values
        snapshot.put("tableId", table.getId());
        snapshot.put("tableNumber", table.getTableNumber());
        snapshot.put("capacity", table.getCapacity());
        snapshot.put("status", table.getState() != null ? table.getState().name() : null);
        snapshot.put("currentGuestCount", table.getCurrentGuestCount());
        snapshot.put("activeOrderCount", table.getActiveOrderCount());

        snapshot.put("branchId", table.getBranch() != null ? table.getBranch().getId() : null);
        snapshot.put("branchName", table.getBranch() != null ? table.getBranch().getName() : null);

        snapshot.put("createdAt", table.getCreatedAt());

        return snapshot;
    }

    /*
     * Gets table branch ID for audit branch filtering.
     */
    private Long getTableBranchId(RestaurantTable table) {
        if (table == null || table.getBranch() == null) {
            return null;
        }

        return table.getBranch().getId();
    }
}