package com.ByteKnights.com.resturarent_system.service.impl.kitchen;

import com.ByteKnights.com.resturarent_system.dto.request.kitchen.CreateChefRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.UpdateStockDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.*;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import com.ByteKnights.com.resturarent_system.service.kitchen.KitchenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KitchenServiceImpl implements KitchenService {

    final OrderRepository orderRepository;
    final OrderItemRepository orderItemRepository;
    final InventoryItemRepository inventoryItemRepository;
    final ChefRequestRepository chefRequestRepository;
    final UserRepository userRepository;
    final StaffRepository staffRepository;

    // kitchen dashboard stat
    @Override
    public KitchenDashboardStatsDTO getKitchenDashboardStats() {

        long pending = orderRepository.countByStatus(OrderStatus.PENDING);
        long preparing = orderRepository.countByStatus(OrderStatus.PREPARING);
        long completed = orderRepository.countByStatus(OrderStatus.COMPLETED);

        Double avgTime = orderRepository.getAveragePreparationTime();
        return new KitchenDashboardStatsDTO(
                pending,
                preparing,
                completed,
                avgTime != null ? Math.round(avgTime * 100.0) / 100.0 : 0.0 // Round to 2 decimal places
        );
    }

    // most popular meals
    @Override
    public List<PopularMealDTO> getMostPopularMealsInLast7Days() {
        List<Object[]> topMeals = orderItemRepository.findTop5PopularMealsInLast7Days();
        Long totalSold = orderItemRepository.getTotalItemsSoldInLast7Days();

        // avoid division by zero
        // return empty list if no meals sold in last 7 days
        if (totalSold == null || totalSold == 0) {
            return new ArrayList<>();
        }

        List<PopularMealDTO> popularMealDtoList = new ArrayList<>();

        for (Object[] row : topMeals) {
            // data extraction
            // row[0] = item_name (String)
            // row[1] = mealCount (Number)
            String mealName = (String) row[0];

            // SQL SUM() typically returns Long/BigInteger. Direct cast Long to Integer will fail.
            // Using 'Number' (the parent class) prevents crashes by safely handling any numeric type.
            Number countNumber = (Number) row[1];
            int count = countNumber.intValue();

            double percentage = (count * 100.0) / totalSold;

            // Mapping to DTO
            popularMealDtoList.add(new PopularMealDTO(
                    mealName,
                    count,
                    Math.round(percentage * 100.0) / 100.0
            ));
        }

        return popularMealDtoList;
    }

    // peak hours
    @Override
    public List<PeakHourDTO> getPeakHoursInLast7Days() {
        // initialize the map with zero counts for all time slots
        Map<String, Integer> peakHourMap = new LinkedHashMap<>();
        peakHourMap.put("8AM-10AM", 0);
        peakHourMap.put("10AM-12PM", 0);
        peakHourMap.put("12PM-2PM", 0);
        peakHourMap.put("2PM-4PM", 0);
        peakHourMap.put("4PM-6PM", 0);
        peakHourMap.put("6PM-8PM", 0);
        peakHourMap.put("8PM-10PM", 0);

        // fetch raw data from the database (Hour, Count)
        List<Object[]> rawData = orderRepository.findOrderCountByHour();

        // map the count from the database to the corresponding time bucket (slot)
        for (Object[] row : rawData) {
            int hour = ((Number) row[0]).intValue();
            int count = ((Number) row[1]).intValue();

            String slot = null;
            if (hour >= 8 && hour < 10) slot = "8AM-10AM";
            else if (hour >= 10 && hour < 12) slot = "10AM-12PM";
            else if (hour >= 12 && hour < 14) slot = "12PM-2PM";
            else if (hour >= 14 && hour < 16) slot = "2PM-4PM";
            else if (hour >= 16 && hour < 18) slot = "4PM-6PM";
            else if (hour >= 18 && hour < 20) slot = "6PM-8PM";
            else if (hour >= 20 && hour < 22) slot = "8PM-10PM";

            if (slot != null) {
                peakHourMap.put(slot, peakHourMap.get(slot) + count);
            }
        }

        // convert the Map to a DTO list for the response
        List<PeakHourDTO> dtos = new ArrayList<>();
        peakHourMap.forEach((time, mealsCount) -> dtos.add(new PeakHourDTO(time, mealsCount)));

        return dtos;
    }

    // inventory alerts
    @Override
    public List<InventoryDetailsDTO> getInventoryAlerts() {
        List<InventoryItem> items = inventoryItemRepository.findAll(); //no need to write native query
        List<InventoryDetailsDTO> alerts = new ArrayList<>();

        for (InventoryItem item : items) {
            double current = item.getQuantity().doubleValue();
            double reorder = item.getReorderLevel().doubleValue();
            double max = item.getMaxStock().doubleValue();

            // send to the dashboard if only current amount <= Reorder level
            if (current <= reorder) {
                String level = (current <= reorder / 2) ? "CRITICAL" : "LOW";

                // Percentage calculation
                double percentage = (max > 0) ? (current / max) * 100 : 0;

                //call the all args constructor of InventoryAlertDTO
                alerts.add(new InventoryDetailsDTO(
                        item.getName(),
                        Math.round(percentage * 100.0) / 100.0, // Round to 2 decimal places
                        max,
                        current,
                        item.getUnit(),
                        level
                ));
            }
        }
        return alerts;
    }

    //get orderCard details
    @Override
    public List<OrderCardDetailsDTO> getOrdersByStatus(OrderStatus status) {
        // Sorting logic (Pending/Preparing = ASC, Completed = DESC)
        Sort sort = (status == OrderStatus.COMPLETED)
                ? Sort.by(Sort.Direction.DESC, "statusUpdatedAt")
                : Sort.by(Sort.Direction.ASC, "statusUpdatedAt");

        List<Order> orders = orderRepository.findByStatus(status, sort);
        List<OrderCardDetailsDTO> orderCardDetailsDTOS = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");

        for (Order order : orders) {
            // Sum of quantities for all items in the order
            int totalQty = order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();

            orderCardDetailsDTOS.add(new OrderCardDetailsDTO(
                    order.getId(),
                    order.getStatus().toString(), // Enum -> String
                    order.getStatusUpdatedAt().format(formatter),
                    totalQty
            ));
        }
        return orderCardDetailsDTOS;
    }

    //get all inventory details
    @Override
    public List<InventoryDetailsDTO> getAllInventoryItems() {
        List<InventoryItem> items = inventoryItemRepository.findAll();
        // can be used same InventoryDetailsDTO for this
        List<InventoryDetailsDTO> dtoList = new ArrayList<>();

        String warningLevel;

        for (InventoryItem item : items) {
            double current = item.getQuantity().doubleValue();
            double max = item.getMaxStock().doubleValue();
            double reorder = item.getReorderLevel().doubleValue();

            if (current <= reorder) {
                warningLevel = (current <= reorder / 2) ? "CRITICAL" : "LOW";
            } else {
                warningLevel = "OK";
            }
            // Percentage calculation
            double percentage = (max > 0) ? (current / max) * 100 : 0;

            //call the all args constructor
            dtoList.add(new InventoryDetailsDTO(
                    item.getName(),
                    Math.round(percentage * 100.0) / 100.0, // Round to 2 decimal places
                    max,
                    current,
                    item.getUnit(),
                    warningLevel // No alert level needed here, so set as NORMAL or you can modify the DTO to make it optional
            ));
        }
        return dtoList;
    }

    // create stock refill requests and new inventory item requests
    @Override
    public void createRequest(CreateChefRequestDTO requestDTO, String userEmail) {

        // get the logged-in User to get the username
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // get the Staff profile to find their branch to identify the specific branch making the request
        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        // create and save the request
        ChefRequest chefRequest = ChefRequest.builder()
                .branch(staff.getBranch())
                .chefName(user.getFullName())
                .itemName(requestDTO.getItemName())
                .requestedQuantity(requestDTO.getRequestedQuantity())
                .unit(requestDTO.getUnit())
                .chefNote(requestDTO.getChefNote())
                .requestType(requestDTO.getRequestType())
                .status(ChefRequestStatus.PENDING)
                .build();

        chefRequestRepository.save(chefRequest);
    }

    //update item count in the inventory
    @Override
    @Transactional // Important when updating the database!
    public void updateInventoryStock(UpdateStockDTO updateDTO) {

        // find the inventory item by its name
        InventoryItem item = inventoryItemRepository.findByName(updateDTO.getItemName())
                .orElseThrow(() -> new RuntimeException("Inventory item not found: " + updateDTO.getItemName()));
        // update the quantity
        item.setQuantity(updateDTO.getNewQuantity());
        // 3. save it back (in the entity already has @PreUpdate to automatically set the lastUpdated time)
        inventoryItemRepository.save(item);
    }

    //get order details
    @Override
    public OrderDetailsDTO getOrderDetails(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        List<OrderItemDetailsDTO> itemDTOs = order.getItems().stream()
                .map(item -> new OrderItemDetailsDTO(
                        item.getId(),
                        item.getItemName(),
                        item.getQuantity(),
                        item.getStatus().toString(),
                        item.getAssignedChef() != null ? item.getAssignedChef().getUser().getFullName() : "Not Assigned"
                )).toList();

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



}