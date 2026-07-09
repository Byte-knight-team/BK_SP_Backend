package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.audit.Auditable;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistTableResponse;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.TableOrderSummary;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.TableReservationSummary;
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
    private final ReservationRepository reservationRepository;
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
        LocalDateTime endOfToday = startOfToday.plusDays(1);

        for (RestaurantTable table : tables) {
            // Always fetch today's orders directly — don't rely on activeOrderCount field
            // which is only updated by the customer service when an order is placed
            // Include today's SERVED orders too — a served-but-unpaid order must stay
            // visible so the receptionist can still collect payment before clearing the table
            List<Order> activeOrders = orderRepository.findByTableIdAndStatusNotIn(
                    table.getId(),
                    List.of(OrderStatus.CANCELLED, OrderStatus.REJECTED)
            ).stream()
                    .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(startOfToday))
                    .toList();

            List<TableOrderSummary> orderSummaries = activeOrders.stream()
                    .map(o -> TableOrderSummary.builder()
                            .orderNumber(o.getOrderNumber())
                            .paymentStatus(o.getPaymentStatus() != null ? o.getPaymentStatus().name() : "PENDING")
                            .orderStatus(o.getStatus() != null ? o.getStatus().name() : null)
                            .readyItemCount((int) o.getItems().stream()
                                    .filter(i -> i.getStatus() == OrderItemStatus.READY)
                                    .count())
                            .finalAmount(o.getFinalAmount() != null ? o.getFinalAmount().doubleValue()
                                    : (o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0))
                            .build())
                    .toList();

            // Check if there is a confirmed reservation for this table today
            List<Reservation> todayReservations = reservationRepository.findByBranchAndDate(
                    branchId, startOfToday, endOfToday);
            TableReservationSummary todayReservation = todayReservations.stream()
                    .filter(r -> r.getTable().getId().equals(table.getId()))
                    .findFirst()
                    .map(r -> TableReservationSummary.builder()
                            .reservationId(r.getId())
                            .customerName(r.getCustomerName())
                            .customerPhone(r.getCustomerPhone())
                            .reservationTime(r.getReservationTime())
                            .endTime(r.getEndTime())
                            .build())
                    .orElse(null);

            ReceptionistTableResponse response = ReceptionistTableResponse.builder()
                    .id(table.getId())
                    .tableNumber(table.getTableNumber())
                    .capacity(table.getCapacity())
                    .status(table.getState())
                    .currentGuestCount(table.getCurrentGuestCount())
                    .activeOrderCount(activeOrders.size())
                    .statusUpdatedAt(table.getStatusUpdatedAt())
                    .activeOrders(orderSummaries)
                    .todayReservation(todayReservation)
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

        // Allow seating on AVAILABLE or RESERVED tables (reserved guests arriving)
        if (table.getState() != TableStatus.AVAILABLE && table.getState() != TableStatus.RESERVED) {
            throw new RuntimeException("Table is not available for seating");
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

        // If a reservation starts within 15 minutes, lock the table as RESERVED instead of AVAILABLE
        LocalDateTime now = LocalDateTime.now();
        boolean hasImmediateReservation = !reservationRepository
                .findOverlappingReservations(tableId, now, now.plusMinutes(15))
                .isEmpty();

        table.setState(hasImmediateReservation ? TableStatus.RESERVED : TableStatus.AVAILABLE);
        table.setCurrentGuestCount(0);
        table.setStatusUpdatedAt(LocalDateTime.now());

        // Save
        tableRepository.save(table);

        webSocketNotificationService.broadcastTableUpdate(branchId);
    }
}