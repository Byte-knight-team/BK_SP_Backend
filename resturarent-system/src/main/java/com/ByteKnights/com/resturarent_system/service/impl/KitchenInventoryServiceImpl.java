package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.audit.Auditable;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.InventoryRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.UpdateDailyRequiredStockDTO;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.UpdateStockDTO;
import com.ByteKnights.com.resturarent_system.dto.response.inventory.ChefRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.InventoryDetailsDTO;
import com.ByteKnights.com.resturarent_system.entity.AuditEventType;
import com.ByteKnights.com.resturarent_system.entity.AuditModule;
import com.ByteKnights.com.resturarent_system.entity.AuditSeverity;
import com.ByteKnights.com.resturarent_system.entity.AuditStatus;
import com.ByteKnights.com.resturarent_system.entity.AuditTargetType;
import com.ByteKnights.com.resturarent_system.entity.ChefRequest;
import com.ByteKnights.com.resturarent_system.entity.ChefRequestStatus;
import com.ByteKnights.com.resturarent_system.entity.InventoryItem;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.repository.ChefRequestRepository;
import com.ByteKnights.com.resturarent_system.repository.InventoryItemRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.KitchenInventoryService;
import com.ByteKnights.com.resturarent_system.service.ManagerNotificationService;
import com.ByteKnights.com.resturarent_system.service.WebSocketNotificationService;
import com.ByteKnights.com.resturarent_system.entity.ManagerNotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KitchenInventoryServiceImpl implements KitchenInventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final ChefRequestRepository chefRequestRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final AuditLogService auditLogService;
    private final ManagerNotificationService managerNotificationService;
    private final WebSocketNotificationService webSocketNotificationService;

    @Override
    public List<InventoryDetailsDTO> getInventoryAlerts(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Long branchId = staff.getBranch().getId();

        List<InventoryItem> items = inventoryItemRepository.findByBranchId(branchId);

        List<InventoryDetailsDTO> alerts = new ArrayList<>();

        for (InventoryItem item : items) {
            double current = item.getQuantity().doubleValue();
            double reorder = item.getReorderLevel().doubleValue();
            double dailyRequired = item.getDailyRequiredStock().doubleValue();

            if (current <= reorder) {
                String level = (current <= reorder / 2) ? "CRITICAL" : "LOW";
                double percentage = (dailyRequired > 0) ? (current / dailyRequired) * 100 : 0;

                alerts.add(new InventoryDetailsDTO(
                        item.getId(),
                        item.getName(),
                        Math.round(percentage * 100.0) / 100.0,
                        dailyRequired,
                        current,
                        item.getUnit(),
                        level
                ));
            }
        }

        return alerts;
    }

    @Override
    public List<InventoryDetailsDTO> getAllInventoryItems(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        List<InventoryItem> items = inventoryItemRepository.findByBranchId(staff.getBranch().getId());

        List<InventoryDetailsDTO> dtoList = new ArrayList<>();

        String warningLevel;

        for (InventoryItem item : items) {
            double current = item.getQuantity().doubleValue();
            double dailyRequired = item.getDailyRequiredStock().doubleValue();
            double reorder = item.getReorderLevel().doubleValue();

            if (current <= reorder) {
                warningLevel = (current <= reorder / 2) ? "CRITICAL" : "LOW";
            } else {
                warningLevel = "OK";
            }

            double percentage = (dailyRequired > 0) ? (current / dailyRequired) * 100 : 0;

            dtoList.add(new InventoryDetailsDTO(
                    item.getId(),
                    item.getName(),
                    Math.round(percentage * 100.0) / 100.0,
                    dailyRequired,
                    current,
                    item.getUnit(),
                    warningLevel
            ));
        }

        return dtoList;
    }

    @Override
    @Auditable(
            module = AuditModule.KITCHEN,
            eventType = AuditEventType.CHEF_REQUEST_CREATED,
            targetType = AuditTargetType.CHEF_REQUEST,
            description = "Chef inventory request created successfully",
            captureResultAsNewValue = false
    )
    @Transactional
    public void createRequest(InventoryRequestDTO requestDTO, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

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
        
        // Notify the manager
        managerNotificationService.createNotification(
                staff.getBranch().getId(),
                ManagerNotificationType.CHEF_REQUEST,
                user.getFullName() + " requested stock for: " + requestDTO.getItemName(),
                chefRequest.getId()
        );
    }

    @Override
    @Transactional
    public void updateInventoryStock(UpdateStockDTO updateDTO, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Long branchId = staff.getBranch().getId();

        InventoryItem item = inventoryItemRepository.findByNameAndBranchId(updateDTO.getItemName(), branchId)
                .orElseThrow(() -> new RuntimeException(
                        "Inventory item not found in your branch: " + updateDTO.getItemName()
                ));

        /*
         * Manual audit is required because this is an important stock update.
         * We capture the stock value before and after the quantity change.
         */
        Map<String, Object> oldValues = buildInventoryItemAuditSnapshot(item);

        item.setQuantity(updateDTO.getNewQuantity());

        InventoryItem savedItem = inventoryItemRepository.save(item);

        checkAndNotifyStockLevel(savedItem);

        auditLogService.logCurrentUserAction(
                AuditModule.INVENTORY,
                AuditEventType.INVENTORY_ITEM_CORRECTED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.INVENTORY_ITEM,
                savedItem.getId(),
                branchId,
                "Inventory stock updated from kitchen successfully",
                oldValues,
                buildInventoryItemAuditSnapshot(savedItem)
        );
    }

    @Override
    @Transactional
    public void updateDailyRequiredStock(UpdateDailyRequiredStockDTO updateDTO, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Long branchId = staff.getBranch().getId();

        InventoryItem item = inventoryItemRepository.findByNameAndBranchId(updateDTO.getItemName(), branchId)
                .orElseThrow(() -> new RuntimeException(
                        "Inventory item not found in your branch: " + updateDTO.getItemName()
                ));

        Map<String, Object> oldValues = buildInventoryItemAuditSnapshot(item);

        item.setDailyRequiredStock(updateDTO.getNewDailyRequiredStock());

        InventoryItem savedItem = inventoryItemRepository.save(item);

        auditLogService.logCurrentUserAction(
                AuditModule.INVENTORY,
                AuditEventType.INVENTORY_ITEM_CORRECTED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.INVENTORY_ITEM,
                savedItem.getId(),
                branchId,
                "Daily required stock updated from kitchen successfully",
                oldValues,
                buildInventoryItemAuditSnapshot(savedItem)
        );
    }

    @Override
    @Transactional
    public void checkAndNotifyStockLevel(InventoryItem item) {
        String level = item.computeStockLevel();

        if (!"OK".equals(level)) {
            if (!item.isLowStockAlerted()) {
                item.setLowStockAlerted(true);
                inventoryItemRepository.save(item);

                webSocketNotificationService.broadcastLowStockAlert(
                        item.getBranch().getId(),
                        item.getId(),
                        item.getName(),
                        level,
                        item.getQuantity().doubleValue(),
                        item.getUnit()
                );
            }
        } else if (item.isLowStockAlerted()) {
            // Restocked back above reorder level — clear the flag so the next dip alerts again.
            item.setLowStockAlerted(false);
            inventoryItemRepository.save(item);
        }
    }

    @Override
    public List<ChefRequestDTO> getMyRequests(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Long branchId = staff.getBranch().getId();

        // Requests are stored with the chef's full name — match this chef's own requests
        List<ChefRequest> requests = chefRequestRepository
                .findByBranchIdAndChefNameOrderByCreatedAtDesc(branchId, user.getFullName());

        List<ChefRequestDTO> dtoList = new ArrayList<>();
        for (ChefRequest req : requests) {
            dtoList.add(toChefRequestDTO(req));
        }
        return dtoList;
    }

    /*
     * Maps a ChefRequest entity to the DTO the frontend expects.
     * The "time" field carries date + time here (requests can span days),
     * and "quantity" combines the amount with its unit (e.g. "20.00 kg").
     */
    private ChefRequestDTO toChefRequestDTO(ChefRequest req) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm");

        String formattedTime = req.getCreatedAt() != null
                ? req.getCreatedAt().format(formatter)
                : "";

        return ChefRequestDTO.builder()
                .id(req.getId())
                .chefName(req.getChefName())
                .time(formattedTime)
                .item(req.getItemName())
                .quantity(req.getRequestedQuantity() + " " + req.getUnit())
                .note(req.getChefNote())
                .managerNote(req.getManagerNote())
                .status(req.getStatus() != null ? req.getStatus().name() : "PENDING")
                .requestType(req.getRequestType() != null ? req.getRequestType().name() : null)
                .build();
    }

    /*
     * Builds a safe audit snapshot for inventory item stock changes.
     */
    private Map<String, Object> buildInventoryItemAuditSnapshot(InventoryItem item) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (item == null) {
            return snapshot;
        }

        snapshot.put("inventoryItemId", item.getId());
        snapshot.put("name", item.getName());
        snapshot.put("quantity", item.getQuantity());
        snapshot.put("unit", item.getUnit());
        snapshot.put("reorderLevel", item.getReorderLevel());
        snapshot.put("dailyRequiredStock", item.getDailyRequiredStock());
        snapshot.put("branchId", item.getBranch() != null ? item.getBranch().getId() : null);
        snapshot.put("branchName", item.getBranch() != null ? item.getBranch().getName() : null);

        return snapshot;
    }
}