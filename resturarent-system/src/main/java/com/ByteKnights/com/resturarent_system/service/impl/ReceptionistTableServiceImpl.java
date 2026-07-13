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

/**
 * Table-floor operations for the receptionist's branch. Loads tables with their live state,
 * active orders and today's reservations, and handles walk-in occupy / guest-count / clear.
 * Every method resolves the caller's branch first and rejects tables from other branches.
 */
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
            // Show only the CURRENT sitting's orders — not previous parties' orders from earlier today.
            // Filters: (1) not CANCELLED/REJECTED/ON_HOLD — held/cancelled orders drop off; SERVED-but-unpaid
            // stays so payment can still be collected; (2) created today; (3) created at/after this sitting
            // started (statusUpdatedAt is set when the table is occupied/seated, untouched by guest-count
            // updates), so a prior occupancy's already-paid orders don't leak into the new party's view.
            LocalDateTime sittingStart = table.getStatusUpdatedAt();
            List<Order> activeOrders = orderRepository.findByTableIdAndStatusNotIn(
                    table.getId(),
                    List.of(OrderStatus.CANCELLED, OrderStatus.REJECTED, OrderStatus.ON_HOLD)
            ).stream()
                    .filter(o -> o.getCreatedAt() != null
                            && o.getCreatedAt().isAfter(startOfToday)
                            && (sittingStart == null || !o.getCreatedAt().isBefore(sittingStart)))
                    .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt())) // oldest first (top)
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

            // All of today's confirmed reservations for this table (ordered by time — the
            // findByBranchAndDate query already sorts by reservationTime ASC)
            List<Reservation> branchTodayReservations = reservationRepository.findByBranchAndDate(
                    branchId, startOfToday, endOfToday);
            List<TableReservationSummary> todayReservations = branchTodayReservations.stream()
                    .filter(r -> r.getTables().stream().anyMatch(t -> t.getId().equals(table.getId())))
                    .map(r -> TableReservationSummary.builder()
                            .reservationId(r.getId())
                            .customerName(r.getCustomerName())
                            .customerPhone(r.getCustomerPhone())
                            .reservationTime(r.getReservationTime())
                            .endTime(r.getEndTime())
                            .build())
                    .toList();

            // If this occupancy came from a reservation, attach that reservation's window.
            TableReservationSummary seatedReservation = null;
            if (table.getSeatedReservationId() != null) {
                seatedReservation = reservationRepository.findById(table.getSeatedReservationId())
                        .map(r -> TableReservationSummary.builder()
                                .reservationId(r.getId())
                                .customerName(r.getCustomerName())
                                .customerPhone(r.getCustomerPhone())
                                .reservationTime(r.getReservationTime())
                                .endTime(r.getEndTime())
                                .build())
                        .orElse(null);
            }

            ReceptionistTableResponse response = ReceptionistTableResponse.builder()
                    .id(table.getId())
                    .tableNumber(table.getTableNumber())
                    .capacity(table.getCapacity())
                    .status(table.getState())
                    .currentGuestCount(table.getCurrentGuestCount())
                    .activeOrderCount(activeOrders.size())
                    .statusUpdatedAt(table.getStatusUpdatedAt())
                    .activeOrders(orderSummaries)
                    .todayReservations(todayReservations)
                    .seatedReservation(seatedReservation)
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

        // Update status and guest count. This is a walk-in seating (no reservation), so clear any link.
        table.setState(TableStatus.OCCUPIED);
        table.setCurrentGuestCount(guestCount);
        table.setSeatedReservationId(null);
        table.setStatusUpdatedAt(LocalDateTime.now());

        // Save the changes
        tableRepository.save(table);

        webSocketNotificationService.broadcastTableUpdate(branchId);
    }

    // update ONLY the guest count of an already-occupied table.
    // Does NOT touch state or statusUpdatedAt, so the "sitting for X" timer keeps running.
    @Override
    @Auditable(
            module = AuditModule.TABLE,
            eventType = AuditEventType.TABLE_STATUS_UPDATED,
            targetType = AuditTargetType.TABLE,
            description = "Table guest count updated successfully",
            captureResultAsNewValue = false
    )
    @Transactional
    public void updateGuestCount(Long tableId, Integer guestCount, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Long branchId = staff.getBranch().getId();

        RestaurantTable table = tableRepository.findByIdForUpdate(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found"));

        if (!table.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Security Alert: Access Denied! This table does not belong to your branch.");
        }

        if (table.getState() != TableStatus.OCCUPIED) {
            throw new RuntimeException("Only an occupied table's guest count can be updated");
        }

        // ONLY the guest count changes — state and seated time are left untouched
        table.setCurrentGuestCount(guestCount);

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

        // Block clearing while any order still needs serving or payment.
        // Cancelled / rejected / on-hold orders are ignored — a held order is heading for
        // cancellation, so it has nothing to serve or collect.
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        boolean allOrdersDone = orderRepository.findByTableIdAndStatusNotIn(
                        tableId, List.of(OrderStatus.CANCELLED, OrderStatus.REJECTED, OrderStatus.ON_HOLD))
                .stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(startOfToday))
                .allMatch(o -> o.getStatus() == OrderStatus.SERVED
                        && o.getPaymentStatus() == PaymentStatus.PAID);

        if (!allOrdersDone) {
            throw new RuntimeException("Cannot clear this table — all orders must be served and paid first.");
        }

        // If a reservation starts within 15 minutes, lock the table as RESERVED instead of AVAILABLE
        LocalDateTime now = LocalDateTime.now();
        boolean hasImmediateReservation = !reservationRepository
                .findOverlappingReservations(tableId, now, now.plusMinutes(15))
                .isEmpty();

        table.setState(hasImmediateReservation ? TableStatus.RESERVED : TableStatus.AVAILABLE);
        table.setCurrentGuestCount(0);
        table.setSeatedReservationId(null);
        table.setStatusUpdatedAt(LocalDateTime.now());

        // Save
        tableRepository.save(table);

        webSocketNotificationService.broadcastTableUpdate(branchId);
    }
}