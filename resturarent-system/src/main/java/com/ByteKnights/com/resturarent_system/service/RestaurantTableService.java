package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.CreateTableRequest;
import com.ByteKnights.com.resturarent_system.dto.TableResponse;
import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.entity.RestaurantTable;
import com.ByteKnights.com.resturarent_system.entity.TableStatus;
import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import com.ByteKnights.com.resturarent_system.repository.RestaurantTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestaurantTableService {

    private final RestaurantTableRepository tableRepository;
    private final BranchRepository branchRepository;

    @Transactional
    public TableResponse createTable(CreateTableRequest request) {
        // 1. Validate branch exists
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException(
                        "Branch not found with id: " + request.getBranchId()));

        // 2. Check for duplicate table number within branch
        if (tableRepository.existsByBranchIdAndTableNumber(
                request.getBranchId(), request.getTableNumber())) {
            throw new RuntimeException(
                    "Table number " + request.getTableNumber()
                            + " already exists in branch " + branch.getName());
        }

        // 3. Parse status (default to AVAILABLE)
        TableStatus status = TableStatus.AVAILABLE;
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                status = TableStatus.valueOf(request.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(
                        "Invalid table status: " + request.getStatus()
                                + ". Valid values: AVAILABLE, OCCUPIED, RESERVED");
            }
        }

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

    @Transactional(readOnly = true)
    public List<TableResponse> getAllTables() {
        return tableRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

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
