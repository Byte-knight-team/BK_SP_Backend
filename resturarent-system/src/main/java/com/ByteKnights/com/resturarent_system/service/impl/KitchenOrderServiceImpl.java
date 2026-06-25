package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.MealCompletionResponseDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.OrderCardDetailsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.OrderDetailsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.OrderItemDetailsDTO;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import com.ByteKnights.com.resturarent_system.service.KitchenOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KitchenOrderServiceImpl implements KitchenOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final ChefAttendanceRepository chefAttendanceRepository;
    private final MenuItemIngredientRepository menuItemIngredientRepository;
    private final InventoryItemRepository inventoryItemRepository;

    @Override
    public List<OrderCardDetailsDTO> getOrdersByStatus(OrderStatus status, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Long branchId = staff.getBranch().getId();
        LocalDateTime startOfToday = LocalDateTime.now().with(LocalTime.MIN);

        Sort sort = (status == OrderStatus.COMPLETED)
                ? Sort.by(Sort.Direction.DESC, "statusUpdatedAt")
                : Sort.by(Sort.Direction.ASC, "statusUpdatedAt");

        List<Order> orders = orderRepository.findByBranchIdAndStatusAndStatusUpdatedAtAfter(
                branchId, status, startOfToday, sort);

        List<OrderCardDetailsDTO> orderCardDetailsDTOS = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");

        for (Order order : orders) {
            int totalQty = 0;
            for (OrderItem item : order.getItems()) {
                totalQty += item.getQuantity();
            }
            orderCardDetailsDTOS.add(new OrderCardDetailsDTO(
                    order.getId(),
                    order.getStatus().name(),
                    order.getStatusUpdatedAt() != null ? order.getStatusUpdatedAt().format(formatter) : null,
                    totalQty
            ));
        }
        return orderCardDetailsDTOS;
    }

    @Override
    public OrderDetailsDTO getOrderDetails(Long orderId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Long branchId = staff.getBranch().getId();

        Order order = orderRepository.findByIdAndBranchId(orderId, branchId)
                .orElseThrow(() -> new RuntimeException("Order not found in your branch with ID: " + orderId));

        List<OrderItemDetailsDTO> itemDTOs = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            itemDTOs.add(new OrderItemDetailsDTO(
                    item.getId(),
                    item.getItemName(),
                    item.getQuantity(),
                    item.getStatus().toString(),
                    item.getAssignedChef() != null ? item.getAssignedChef().getUser().getFullName() : "Not Assigned"
            ));
        }

        return new OrderDetailsDTO(
                order.getId(),
                order.getCreatedAt(),
                order.getStatusUpdatedAt(),
                order.getStatus().toString(),
                order.getHoldReason(),
                order.getKitchenNotes(),
                itemDTOs
        );
    }

    @Override
    public void assignChefToMeal(Long orderItemId, Long chefStaffId) {
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("Meal not found"));

        Staff chef = staffRepository.findById(chefStaffId)
                .orElseThrow(() -> new RuntimeException("Chef not found"));

        if (!item.getOrder().getBranch().getId().equals(chef.getBranch().getId())) {
            throw new RuntimeException("Security Alert: Cannot assign a chef from a different branch!");
        }

        item.setAssignedChef(chef);
        orderItemRepository.save(item);
    }

    @Override
    @Transactional
    public void startMeal(Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Meal item not found"));

        if (item.getAssignedChef() == null) {
            throw new RuntimeException("Cannot start meal: No chef assigned yet!");
        }

        // --- STOCK CHECK ---
        // If this order item has a linked menu item with a defined recipe,
        // verify there is enough stock for every ingredient before starting
        if (item.getMenuItem() != null) {
            List<MenuItemIngredient> ingredients =
                    menuItemIngredientRepository.findByMenuItemId(item.getMenuItem().getId());

            if (!ingredients.isEmpty()) {
                // Build a readable error message listing every ingredient that is short
                List<String> shortages = new ArrayList<>();

                for (MenuItemIngredient ingredient : ingredients) {
                    // Total needed = quantity per serving × number of servings ordered
                    BigDecimal needed = ingredient.getQuantityRequired()
                            .multiply(BigDecimal.valueOf(item.getQuantity()));

                    InventoryItem stock = ingredient.getInventoryItem();
                    BigDecimal available = stock.getQuantity() != null ? stock.getQuantity() : BigDecimal.ZERO;

                    if (available.compareTo(needed) < 0) {
                        shortages.add(String.format("%s: need %.3f %s, have %.3f %s",
                                stock.getName(), needed, stock.getUnit(), available, stock.getUnit()));
                    }
                }

                // If any shortages found, throw an error with full details for the frontend
                if (!shortages.isEmpty()) {
                    throw new RuntimeException("INSUFFICIENT_STOCK:" + String.join("|", shortages));
                }

                // --- STOCK DEDUCTION ---
                // All stock checks passed — deduct the required quantities
                for (MenuItemIngredient ingredient : ingredients) {
                    BigDecimal needed = ingredient.getQuantityRequired()
                            .multiply(BigDecimal.valueOf(item.getQuantity()));

                    InventoryItem stock = ingredient.getInventoryItem();
                    stock.setQuantity(stock.getQuantity().subtract(needed));
                    inventoryItemRepository.save(stock);
                }
            }
        }

        // --- PROCEED WITH MEAL START ---
        item.setStatus(OrderItemStatus.PREPARING);
        item.setCookingStartedAt(LocalDateTime.now());
        orderItemRepository.save(item);

        Order order = item.getOrder();
        if (order.getStatus() == OrderStatus.PENDING) {
            order.updateStatus(OrderStatus.PREPARING);
            orderRepository.save(order);
        }

        ChefAttendance attendance = chefAttendanceRepository.findByStaffIdAndAttendanceDate(
                        item.getAssignedChef().getId(), LocalDate.now())
                .orElseThrow(() -> new RuntimeException("Chef attendance record not found for today"));

        if (attendance.getAttendanceStatus() == ChefAttendanceStatus.OFF_DUTY) {
            throw new RuntimeException("Cannot start: Chef " +
                    item.getAssignedChef().getUser().getFullName() + " has already checked out!");
        }
        if (attendance.getWorkStatus() == ChefWorkStatus.ON_BREAK) {
            throw new RuntimeException("Cannot start: Chef is currently on a break.");
        }

        attendance.setWorkStatus(ChefWorkStatus.COOKING);
        chefAttendanceRepository.save(attendance);
    }

    @Override
    @Transactional
    public MealCompletionResponseDTO completeMeal(Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Meal item not found"));

        item.setStatus(OrderItemStatus.READY);
        item.setCookingCompletedAt(LocalDateTime.now());
        orderItemRepository.save(item);

        ChefAttendance attendance = chefAttendanceRepository.findByStaffIdAndAttendanceDate(
                        item.getAssignedChef().getId(), LocalDate.now())
                .orElseThrow(() -> new RuntimeException("Chef attendance not found"));

        attendance.setWorkStatus(ChefWorkStatus.AVAILABLE);
        chefAttendanceRepository.save(attendance);

        Order order = item.getOrder();
        boolean allFinished = order.getItems().stream()
                .allMatch(i -> i.getStatus() == OrderItemStatus.READY);

        if (allFinished) {
            order.updateStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);
        }

        return new MealCompletionResponseDTO(order.getStatus().toString());
    }

    @Override
    @Transactional
    public void holdOrder(Long orderId, String holdReason, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Long branchId = staff.getBranch().getId();

        Order order = orderRepository.findByIdAndBranchId(orderId, branchId)
                .orElseThrow(() -> new RuntimeException("Order not found in your branch with ID: " + orderId));

        order.setStatus(OrderStatus.ON_HOLD);
        order.setHoldReason(holdReason);
        order.setStatusUpdatedAt(LocalDateTime.now());

        for (OrderItem item : order.getItems()) {
            item.setStatus(OrderItemStatus.ON_HOLD);
        }

        orderRepository.save(order);
    }
}
