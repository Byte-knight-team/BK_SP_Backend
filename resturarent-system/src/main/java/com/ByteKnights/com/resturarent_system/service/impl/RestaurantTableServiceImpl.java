package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.admin.CreateTableRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.UpdateTableRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.UpdateTableStatusRequest;
import com.ByteKnights.com.resturarent_system.dto.response.admin.TableResponse;
import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.entity.RestaurantTable;
import com.ByteKnights.com.resturarent_system.entity.TableStatus;
import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import com.ByteKnights.com.resturarent_system.repository.QrCodeRepository;
import com.ByteKnights.com.resturarent_system.repository.RestaurantTableRepository;
import com.ByteKnights.com.resturarent_system.service.RestaurantTableService;
import com.ByteKnights.com.resturarent_system.exception.ResourceNotFoundException;
import com.ByteKnights.com.resturarent_system.exception.DuplicateResourceException;
import com.ByteKnights.com.resturarent_system.exception.InvalidOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implements table management business rules.
 */
@Service
@RequiredArgsConstructor
public class RestaurantTableServiceImpl implements RestaurantTableService {

    private final RestaurantTableRepository tableRepository;
    private final BranchRepository branchRepository;
    private final QrCodeRepository qrCodeRepository;

    /**
     * Creates a table after validating branch state and uniqueness.
     */
    @Override
    @Transactional
    public TableResponse createTable(CreateTableRequest request) {
        // 1. Validate branch exists
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Branch not found with id: " + request.getBranchId()));

        if (branch.getStatus() != com.ByteKnights.com.resturarent_system.entity.BranchStatus.ACTIVE) {
            throw new InvalidOperationException("Cannot create table in an inactive branch: " + branch.getName());
        }

        // 2. Check for duplicate table number within branch
        if (tableRepository.existsByBranchIdAndTableNumber(
                request.getBranchId(), request.getTableNumber())) {
            throw new DuplicateResourceException(
                    "Table number " + request.getTableNumber()
                            + " already exists in branch " + branch.getName());
        }

        // 3. Parse status (default to AVAILABLE)
        TableStatus status = parseStatus(request.getStatus());

        // 4. Build and save entity
        RestaurantTable table = RestaurantTable.builder()
                .branch(branch)
                .tableNumber(request.getTableNumber())
                .capacity(request.getCapacity())
                .state(status)
                .build();

        RestaurantTable saved = tableRepository.save(table);
        return mapToResponse(saved);
    }

    /**
     * Fetches one table by id.
     */
    @Override
    @Transactional(readOnly = true)
    public TableResponse getTableById(Long id) {
        RestaurantTable table = findTableOrThrow(id);
        return mapToResponse(table);
    }

    /**
     * Returns all tables.
     */
    @Override
    @Transactional(readOnly = true)
    public List<TableResponse> getAllTables() {
        return tableRepository.findAll()
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

        // Update table number if provided
        if (request.getTableNumber() != null) {
            // Check for duplicate table number within same branch (exclude current table)
            if (!table.getTableNumber().equals(request.getTableNumber())
                    && tableRepository.existsByBranchIdAndTableNumberAndIdNot(
                            table.getBranch().getId(), request.getTableNumber(), id)) {
                throw new DuplicateResourceException(
                        "Table number " + request.getTableNumber()
                                + " already exists in branch " + table.getBranch().getName());
            }
            table.setTableNumber(request.getTableNumber());
        }

        // Update capacity if provided
        if (request.getCapacity() != null) {
            table.setCapacity(request.getCapacity());
        }

        // Update status if provided
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            table.setState(parseStatus(request.getStatus()));
        }

        RestaurantTable updated = tableRepository.save(table);
        return mapToResponse(updated);
    }

    /**
     * Updates only the state of a table.
     */
    @Override
    @Transactional
    public TableResponse updateTableStatus(Long id, UpdateTableStatusRequest request) {
        RestaurantTable table = findTableOrThrow(id);

        // Capture the current persisted active order count before applying manual status input.
        // This lets us enforce a business rule for the common transition from 0 -> 1 active orders.
        Integer activeOrderCount = table.getActiveOrderCount() == null ? 0 : table.getActiveOrderCount();

        // Apply the requested status first so administrators can still drive explicit state changes.
        table.setState(parseStatus(request.getStatus()));

        // Automatic status sync rules:
        // 1) If active orders are present, AVAILABLE must transition to OCCUPIED.
        // 2) If active orders are zero, OCCUPIED must transition back to AVAILABLE.
        // These rules keep table state consistent with real-time order load.
        if (table.getState() == TableStatus.AVAILABLE && activeOrderCount > 0) {
            table.setState(TableStatus.OCCUPIED);
        } else if (table.getState() == TableStatus.OCCUPIED && activeOrderCount == 0) {
            table.setState(TableStatus.AVAILABLE);
        }

        RestaurantTable updated = tableRepository.save(table);
        return mapToResponse(updated);
    }

    /**
     * Deletes a table only when it is safe to remove.
     */
    @Override
    @Transactional
    public void deleteTable(Long id) {
        RestaurantTable table = findTableOrThrow(id);

        // Enforce a hard business rule: once a table has QR references, the row must not be
        // physically deleted. This keeps QR audit/history intact and avoids database FK errors
        // from qr_codes.table_id -> restaurant_tables.id.
        if (qrCodeRepository.existsByTableId(id)) {
            throw new InvalidOperationException(
                    "Cannot delete table: QR code history exists for this table. "
                            + "Revoke and clean up QR records first.");
        }

        if (table.getActiveOrderCount() != null && table.getActiveOrderCount() > 0) {
            throw new InvalidOperationException("Cannot delete table: active orders exist");
        }

        if (table.getState() == TableStatus.OCCUPIED) {
            throw new InvalidOperationException("Cannot delete: The table is occupied");
        }

        if (table.getState() == TableStatus.RESERVED) {
            throw new InvalidOperationException("Cannot delete: The table is reserved");
        }

        tableRepository.delete(table);
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
}
