package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistTableResponse;
import com.ByteKnights.com.resturarent_system.entity.RestaurantTable;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.repository.RestaurantTableRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.service.ReceptionistTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
