package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.audit.Auditable;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistTableResponse;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import com.ByteKnights.com.resturarent_system.service.ReceptionistTableService;
import com.ByteKnights.com.resturarent_system.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceptionistTableServiceImpl implements ReceptionistTableService {

    private final RestaurantTableRepository tableRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final OrderRepository orderRepository;
    private final WebSocketNotificationService webSocketNotificationService;

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

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        for (RestaurantTable table : tables) {
            // Always fetch today's orders directly — don't rely on activeOrderCount field
            // which is only updated by the customer service when an order is placed
            List<Order> activeOrders = orderRepository.findByTableIdAndStatusNotIn(
                    table.getId(),
                    List.of(OrderStatus.CANCELLED, OrderStatus.REJECTED, OrderStatus.SERVED)
            ).stream()
                    .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(startOfToday))
                    .toList();

            List<String> orderNumbers = activeOrders.stream()
                    .map(Order::getOrderNumber)
                    .toList();

            ReceptionistTableResponse response = ReceptionistTableResponse.builder()
                    .id(table.getId())
                    .tableNumber(table.getTableNumber())
                    .capacity(table.getCapacity())
                    .status(table.getState())
                    .currentGuestCount(table.getCurrentGuestCount())
                    .activeOrderCount(activeOrders.size())
                    .statusUpdatedAt(table.getStatusUpdatedAt())
                    .activeOrderIds(orderNumbers)
                    .build();

            dtoList.add(response);
        }

        return dtoList;
    }

    // mark a table as OCCUPIED and set the guest count
    @Override
    @Auditable(
            module = AuditModule.TABLE,
            eventType = AuditEventType.TABLE_STATUS_UPDATED,
            targetType = AuditTargetType.TABLE,
            description = "Table marked as occupied successfully",
            captureResultAsNewValue = false
    )
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

        webSocketNotificationService.broadcastTableUpdate(branchId);
    }

    // mark a table as AVAILABLE and reset guest count
    @Override
    @Auditable(
            module = AuditModule.TABLE,
            eventType = AuditEventType.TABLE_STATUS_UPDATED,
            targetType = AuditTargetType.TABLE,
            description = "Table cleared successfully",
            captureResultAsNewValue = false
    )
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

        webSocketNotificationService.broadcastTableUpdate(branchId);
    }
}