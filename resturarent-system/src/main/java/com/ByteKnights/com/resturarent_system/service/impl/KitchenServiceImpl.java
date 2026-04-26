package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.kitchen.InventoryRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.UpdateStockDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.*;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import com.ByteKnights.com.resturarent_system.service.KitchenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    final ChefAttendanceRepository chefAttendanceRepository;

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

        // Define the time range: Only fetch orders starting from today's midnight
        LocalDateTime startOfToday = LocalDateTime.now().with(LocalTime.MIN);

        // Sorting logic (Pending/Preparing = ASC, Completed = DESC)
        Sort sort = (status == OrderStatus.COMPLETED)
                ? Sort.by(Sort.Direction.DESC, "statusUpdatedAt")
                : Sort.by(Sort.Direction.ASC, "statusUpdatedAt");

        // Retrieve sorted orders from today
        List<Order> orders = orderRepository.findByStatusAndStatusUpdatedAtAfter(status, startOfToday, sort);

        // Initialize a list to hold the DTOs for the frontend
        List<OrderCardDetailsDTO> orderCardDetailsDTOS = new ArrayList<>();

        // Define a 12-hour time format (e.g., 08:30 PM) for better readability
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");

        // Nested Looping to calculate total item count for each order and map to DTO
        // loop through all filtered order
        for (Order order : orders) {
            // Set initial count as Zero
            int totalQty = 0;
            // loop through sll items in the selected order
            for (OrderItem item : order.getItems()) {
                // calculate total item count in the selected order
                totalQty = totalQty + item.getQuantity();
            }

            // add OrderCardDetailsDTO list to the array list using .add() method and call the all args constructor of OrderCardDetailsDTO
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
    public void createRequest(InventoryRequestDTO requestDTO, String userEmail) {

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

    //get all available Line chefs from the same branch of the logged-in Chief Chef to assign them to prepare meals
    @Override
    public List<ChefAssignDTO> getAvailableChefsForAssignment(String userEmail) {
        // Find logged-in user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        // Find Chief Chef's branch
        Staff currentStaff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));
        Long branchId = currentStaff.getBranch().getId();

        // Define filters
        // Logic to filter Line Chefs:
        // 1. Same Branch: Only chefs working at the same location as the logged-in Chief Chef.
        // 2. Today's Attendance: Must have a recorded attendance entry for the current date.
        // 3. On-Duty Only: The chef must be currently clocked-in (Attendance Status: ON_DUTY).
        // 4. Working Status: Must be either currently 'AVAILABLE' (idle) or 'COOKING' (already assigned to a meal).
        LocalDate today = LocalDate.now();
        List<ChefWorkStatus> allowedStatuses = List.of(ChefWorkStatus.AVAILABLE, ChefWorkStatus.COOKING);
        // Fetch raw Entities from the database
        List<ChefAttendance> attendances = chefAttendanceRepository
                .findAvailableLineChefsForBranch(
                        today, branchId, ChefAttendanceStatus.ON_DUTY, allowedStatuses
                );
        // Map the Entities to your ChefAssignDTO and return
        return attendances.stream()
                .map(ca -> new ChefAssignDTO(
                        ca.getStaff().getId(),
                        ca.getStaff().getUser().getFullName(),
                        ca.getWorkStatus().toString()
                )).toList();
    }

    // Save the assigned chef in the order_items table
    @Override
    public void assignChefToMeal(Long orderItemId, Long chefStaffId) {
        // Find the meal (OrderItem)
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("Meal not found"));

        // Find the chosen chef (Staff)
        Staff chef = staffRepository.findById(chefStaffId)
                .orElseThrow(() -> new RuntimeException("Chef not found"));

        // Link the chef to the meal and save
        item.setAssignedChef(chef);
        orderItemRepository.save(item);
    }

    //get all the Line Chefs Who is not checked in yet
    @Override
    public List<ChefCheckInDTO> getLineChefsForCheckIn(String userEmail) {
        // Get logged-in Chief Chef
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff currentStaff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        // Fetch line chefs who haven't checked in today at this branch from database
        List<Staff> staffList = staffRepository.findLineChefsNotCheckedInToday(currentStaff.getBranch().getId(), LocalDate.now());

        // Create a new empty list for our DTOs
        List<ChefCheckInDTO> checkInList = new ArrayList<>();

        // Loop through each Staff and convert to ChefCheckInDTO
        for (Staff s : staffList) {
            ChefCheckInDTO dto = new ChefCheckInDTO();
            dto.setId(s.getId());
            dto.setFullName(s.getUser().getFullName());

            // Add to our list
            checkInList.add(dto);
        }

        // Return the final list
        return checkInList;
    }

    // save check in attendance record
    @Override
    @Transactional // This is important because we are saving to the database!
    public void checkInChef(Long chefId) {

        // Check if already checked in today
        boolean alreadyCheckedIn = chefAttendanceRepository.existsByStaffIdAndAttendanceDate(chefId, LocalDate.now());

        if (alreadyCheckedIn) {
            throw new RuntimeException("Chef has already checked in for today!");
        }

        // Find the chef by the ID sent from the frontend
        Staff chef = staffRepository.findById(chefId)
                .orElseThrow(() -> new RuntimeException("Chef not found"));

        // Create a new attendance record
        ChefAttendance attendance = new ChefAttendance();
        attendance.setStaff(chef);
        attendance.setAttendanceDate(LocalDate.now()); // Sets today's date
        attendance.setClockInTime(LocalDateTime.now()); // Sets the exact time right now
        attendance.setAttendanceStatus(ChefAttendanceStatus.ON_DUTY); // now the chef is clocked in and ready to work
        attendance.setWorkStatus(ChefWorkStatus.AVAILABLE); // not started cooking yet

        // Save it to the database
        chefAttendanceRepository.save(attendance);
    }

}