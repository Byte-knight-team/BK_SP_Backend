package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistTableResponse;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.ReceptionistTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReceptionistTableServiceImpl implements ReceptionistTableService {

    private final RestaurantTableRepository tableRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final OrderRepository orderRepository;
    private final AuditLogService auditLogService;

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

        for (RestaurantTable table : tables) {
            // Build base response
            ReceptionistTableResponse response = ReceptionistTableResponse.builder()
                    .id(table.getId())
                    .tableNumber(table.getTableNumber())
                    .capacity(table.getCapacity())
                    .status(table.getState())
                    .currentGuestCount(table.getCurrentGuestCount())
                    .activeOrderCount(table.getActiveOrderCount())
                    .statusUpdatedAt(table.getStatusUpdatedAt())
                    .build();

            // If the table has active orders, fetch them
            if (table.getActiveOrderCount() != null && table.getActiveOrderCount() > 0) {

                // Fetch ALL orders for this table EXCEPT Cancelled or Rejected ones
                List<Order> activeOrders = orderRepository.findByTableIdAndStatusNotIn(
                        table.getId(),
                        List.of(OrderStatus.CANCELLED, OrderStatus.REJECTED)
                );

                // Create a list of just the Order Strings (e.g. "#ORD-482")
                List<String> orderNumbers = new ArrayList<>();

                for (Order order : activeOrders) {
                    orderNumbers.add(order.getOrderNumber());
                }

                // Attach the real order numbers to the response
                response.setActiveOrderIds(orderNumbers);
            } else {
                // If no active orders, attach an empty list
                response.setActiveOrderIds(new ArrayList<>());
            }

            dtoList.add(response);
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

        /*
         * Manual audit is used because this method changes table status and guest count.
         * oldValuesJson shows the previous table state.
         */
        Map<String, Object> oldValues = buildTableAuditSnapshot(table);

        // Update status and guest count
        table.setState(TableStatus.OCCUPIED);
        table.setCurrentGuestCount(guestCount);
        table.setStatusUpdatedAt(LocalDateTime.now());

        // Save the changes
        RestaurantTable savedTable = tableRepository.save(table);

        auditLogService.logCurrentUserAction(
                AuditModule.TABLE,
                AuditEventType.TABLE_STATUS_UPDATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.TABLE,
                savedTable.getId(),
                branchId,
                "Table marked as occupied successfully",
                oldValues,
                buildTableAuditSnapshot(savedTable)
        );
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

        /*
         * Manual audit is used because this method changes table status and guest count.
         */
        Map<String, Object> oldValues = buildTableAuditSnapshot(table);

        // Reset table data
        table.setState(TableStatus.AVAILABLE);
        table.setCurrentGuestCount(0);
        table.setStatusUpdatedAt(LocalDateTime.now());

        // Save
        RestaurantTable savedTable = tableRepository.save(table);

        auditLogService.logCurrentUserAction(
                AuditModule.TABLE,
                AuditEventType.TABLE_STATUS_UPDATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.TABLE,
                savedTable.getId(),
                branchId,
                "Table cleared successfully",
                oldValues,
                buildTableAuditSnapshot(savedTable)
        );
    }

    /*
     * Builds a safe audit snapshot for table status changes.
     * We store only useful fields instead of storing the whole entity object.
     */
    private Map<String, Object> buildTableAuditSnapshot(RestaurantTable table) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (table == null) {
            return snapshot;
        }

        snapshot.put("tableId", table.getId());
        snapshot.put("tableNumber", table.getTableNumber());
        snapshot.put("capacity", table.getCapacity());
        snapshot.put("status", table.getState() != null ? table.getState().name() : null);
        snapshot.put("currentGuestCount", table.getCurrentGuestCount());
        snapshot.put("activeOrderCount", table.getActiveOrderCount());
        snapshot.put("statusUpdatedAt", table.getStatusUpdatedAt());
        snapshot.put("branchId", table.getBranch() != null ? table.getBranch().getId() : null);
        snapshot.put("branchName", table.getBranch() != null ? table.getBranch().getName() : null);

        return snapshot;
    }
}