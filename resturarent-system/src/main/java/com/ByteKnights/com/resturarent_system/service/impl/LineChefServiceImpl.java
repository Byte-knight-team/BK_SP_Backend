package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.LineChefItemDTO;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import com.ByteKnights.com.resturarent_system.service.LineChefService;
import com.ByteKnights.com.resturarent_system.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LineChefServiceImpl implements LineChefService {

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final ChefAttendanceRepository chefAttendanceRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private Staff getStaffFromEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));
    }

    @Override
    public List<LineChefItemDTO> getMyItems(String userEmail) {
        Staff lineChef = getStaffFromEmail(userEmail);

        List<OrderItem> activeItems = orderItemRepository.findByAssignedLineChefIdAndStatusIn(
                lineChef.getId(),
                List.of(OrderItemStatus.PENDING, OrderItemStatus.PREPARING)
        );

        // Include today's READY and SERVED items (SERVED = already handed to customer)
        // Both are filtered by cookingCompletedAt to avoid historical data
        List<OrderItem> todayDoneItems = orderItemRepository.findByAssignedLineChefIdAndStatusIn(
                lineChef.getId(),
                List.of(OrderItemStatus.READY, OrderItemStatus.SERVED)
        ).stream()
                .filter(item -> item.getCookingCompletedAt() != null &&
                        item.getCookingCompletedAt().toLocalDate().equals(LocalDate.now()))
                .toList();

        List<OrderItem> items = new ArrayList<>();
        items.addAll(activeItems);
        items.addAll(todayDoneItems);

        return items.stream().map(item -> {
            Order order = item.getOrder();
            Integer tableNumber = order.getTable() != null ? order.getTable().getTableNumber() : null;
            String placedAt = order.getCreatedAt() != null ? order.getCreatedAt().format(FORMATTER) : "";

            return new LineChefItemDTO(
                    item.getId(),
                    item.getItemName(),
                    item.getQuantity(),
                    item.getStatus().name(),
                    order.getId(),
                    order.getOrderNumber(),
                    order.getOrderType().name(),
                    tableNumber,
                    placedAt,
                    item.getKitchenNotes(),
                    order.getKitchenNotes()
            );
        }).toList();
    }

    @Override
    @Transactional
    public void startItem(Long itemId, String userEmail) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        Staff lineChef = getStaffFromEmail(userEmail);

        if (item.getAssignedLineChef() == null ||
                !item.getAssignedLineChef().getId().equals(lineChef.getId())) {
            throw new RuntimeException("This item is not assigned to you");
        }

        if (item.getStatus() != OrderItemStatus.PENDING) {
            throw new RuntimeException("Item is not in PENDING status");
        }

        item.setStatus(OrderItemStatus.PREPARING);
        item.setCookingStartedAt(LocalDateTime.now());
        orderItemRepository.save(item);

        Order order = item.getOrder();
        if (order.getStatus() == OrderStatus.PENDING) {
            order.updateStatus(OrderStatus.PREPARING);
            orderRepository.save(order);
        }

        ChefAttendance attendance = chefAttendanceRepository
                .findByStaffIdAndAttendanceDate(lineChef.getId(), LocalDate.now())
                .orElseThrow(() -> new RuntimeException("No attendance record for today. Please check in first."));
        attendance.setWorkStatus(ChefWorkStatus.COOKING);
        chefAttendanceRepository.save(attendance);

        Long branchId = order.getBranch() != null ? order.getBranch().getId() : null;
        if (branchId != null) {
            webSocketNotificationService.broadcastKitchenItemUpdate(branchId, order.getId(), order.getOrderNumber(), item.getItemName(), "PREPARING", order.getStatus().name(), order.getOrderType().name());
        }
    }

    @Override
    @Transactional
    public void completeItem(Long itemId, String userEmail) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        Staff lineChef = getStaffFromEmail(userEmail);

        if (item.getAssignedLineChef() == null ||
                !item.getAssignedLineChef().getId().equals(lineChef.getId())) {
            throw new RuntimeException("This item is not assigned to you");
        }

        if (item.getStatus() != OrderItemStatus.PREPARING) {
            throw new RuntimeException("Item is not being prepared");
        }

        item.setStatus(OrderItemStatus.READY);
        item.setCookingCompletedAt(LocalDateTime.now());
        orderItemRepository.save(item);

        // If all items in the order are READY → mark order COMPLETED
        Order order = item.getOrder();
        boolean allFinished = order.getItems().stream()
                .allMatch(i -> i.getStatus() == OrderItemStatus.READY
                        || i.getStatus() == OrderItemStatus.SERVED);
        if (allFinished) {
            order.updateStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);
        }

        Long branchId = order.getBranch() != null ? order.getBranch().getId() : null;
        if (branchId != null) {
            webSocketNotificationService.broadcastKitchenItemUpdate(branchId, order.getId(), order.getOrderNumber(), item.getItemName(), "READY", order.getStatus().name(), order.getOrderType().name());
            // QR order: refresh the receptionist table monitor so "Ready to serve" appears live
            if (order.getOrderType() == OrderType.QR) {
                webSocketNotificationService.broadcastTableUpdate(branchId);
            }
        }

        // If this line chef has no more PREPARING items → set status back to AVAILABLE
        List<OrderItem> stillPreparing = orderItemRepository.findByAssignedLineChefIdAndStatusIn(
                lineChef.getId(), List.of(OrderItemStatus.PREPARING));
        if (stillPreparing.isEmpty()) {
            ChefAttendance attendance = chefAttendanceRepository
                    .findByStaffIdAndAttendanceDate(lineChef.getId(), LocalDate.now())
                    .orElseThrow(() -> new RuntimeException("No attendance record found"));
            attendance.setWorkStatus(ChefWorkStatus.AVAILABLE);
            chefAttendanceRepository.save(attendance);
        }
    }
}
