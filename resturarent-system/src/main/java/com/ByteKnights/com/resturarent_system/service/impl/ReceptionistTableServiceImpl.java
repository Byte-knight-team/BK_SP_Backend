package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistTableResponse;
import com.ByteKnights.com.resturarent_system.entity.RestaurantTable;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.entity.TableStatus;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.repository.RestaurantTableRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.service.ReceptionistTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceptionistTableServiceImpl implements ReceptionistTableService {

    final RestaurantTableRepository tableRepository;
    final UserRepository userRepository;
    final StaffRepository staffRepository;

    // fetch all tables belonging to the receptionist's branch
    @Override
    public List<ReceptionistTableResponse> getBranchTables(String userEmail) {

        // Get the logged-in User
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Find the Staff profile to identify their branch
        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Long branchId = staff.getBranch().getId();

        // Fetch all tables registered at this specific branch
        List<RestaurantTable> tables = tableRepository.findByBranchId(branchId);

        List<ReceptionistTableResponse> dtoList = new ArrayList<>();

        // Loop through each table to map it to the Response DTO
        for (RestaurantTable table : tables) {

            // Map entity data to the builder
            dtoList.add(ReceptionistTableResponse.builder()
                    .id(table.getId())
                    .tableNumber(table.getTableNumber())
                    .capacity(table.getCapacity())
                    .status(table.getState()) // State is the field name in entity
                    .currentGuestCount(table.getCurrentGuestCount())
                    .activeOrderCount(table.getActiveOrderCount())
                    .statusUpdatedAt(table.getStatusUpdatedAt())
                    .build());
        }
        return dtoList;
    }

    // mark a table as OCCUPIED and set the guest count
    @Override
    @Transactional
    public void occupyTable(Long tableId, Integer guestCount, String userEmail) {

        // Identify the branch
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));
        Long branchId = staff.getBranch().getId();

        // Find the table and use a Lock for safety
        RestaurantTable table = tableRepository.findByIdForUpdate(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found"));

        // Security Check: Ensure table belongs to the same branch
        if (!table.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Security Alert: Access Denied! This table does not belong to your branch.");
        }

        // Update status and guest count
        table.setState(TableStatus.OCCUPIED);
        table.setCurrentGuestCount(guestCount);
        table.setStatusUpdatedAt(LocalDateTime.now());

        // Save the changes
        tableRepository.save(table);
    }

    // mark a table as AVAILABLE and reset guest count
    @Override
    @Transactional
    public void clearTable(Long tableId, String userEmail) {

        // Identify the branch
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));
        Long branchId = staff.getBranch().getId();

        // Find the table with a Lock
        RestaurantTable table = tableRepository.findByIdForUpdate(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found"));

        // Security Check: Ensure table belongs to the same branch
        if (!table.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Security Alert: Access Denied! This table does not belong to your branch.");
        }

        // Reset table data
        table.setState(TableStatus.AVAILABLE);
        table.setCurrentGuestCount(0);
        table.setStatusUpdatedAt(LocalDateTime.now());

        // Save
        tableRepository.save(table);
    }

}
