package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.kitchen.InventoryRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.UpdateStockDTO;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            double max = item.getMaxStock().doubleValue();

            if (current <= reorder) {
                String level = (current <= reorder / 2) ? "CRITICAL" : "LOW";
                double percentage = (max > 0) ? (current / max) * 100 : 0;

                alerts.add(new InventoryDetailsDTO(
                        item.getId(),
                        item.getName(),
                        Math.round(percentage * 100.0) / 100.0,
                        max,
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
            double max = item.getMaxStock().doubleValue();
            double reorder = item.getReorderLevel().doubleValue();

            if (current <= reorder) {
                warningLevel = (current <= reorder / 2) ? "CRITICAL" : "LOW";
            } else {
                warningLevel = "OK";
            }

            double percentage = (max > 0) ? (current / max) * 100 : 0;

            dtoList.add(new InventoryDetailsDTO(
                    item.getId(),
                    item.getName(),
                    Math.round(percentage * 100.0) / 100.0,
                    max,
                    current,
                    item.getUnit(),
                    warningLevel
            ));
        }

        return dtoList;
    }

    @Override
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

        ChefRequest savedRequest = chefRequestRepository.save(chefRequest);

        /*
         * Manual audit is used because this method returns void.
         * This keeps the created chef request ID, branch ID, and request details in audit_logs.
         */
        auditLogService.logCurrentUserAction(
                AuditModule.KITCHEN,
                AuditEventType.CHEF_REQUEST_CREATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.CHEF_REQUEST,
                savedRequest.getId(),
                staff.getBranch().getId(),
                "Chef inventory request created successfully",
                null,
                buildChefRequestAuditSnapshot(savedRequest)
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
         * Manual audit is required because this is an update operation.
         * We capture the stock value before and after the quantity change.
         */
        Map<String, Object> oldValues = buildInventoryItemAuditSnapshot(item);

        item.setQuantity(updateDTO.getNewQuantity());

        InventoryItem savedItem = inventoryItemRepository.save(item);

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

    /*
     * Builds a safe audit snapshot for a chef inventory request.
     * We store only useful fields instead of saving the full entity object.
     */
    private Map<String, Object> buildChefRequestAuditSnapshot(ChefRequest request) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (request == null) {
            return snapshot;
        }

        snapshot.put("chefRequestId", request.getId());
        snapshot.put("branchId", request.getBranch() != null ? request.getBranch().getId() : null);
        snapshot.put("branchName", request.getBranch() != null ? request.getBranch().getName() : null);
        snapshot.put("chefName", request.getChefName());
        snapshot.put("itemName", request.getItemName());
        snapshot.put("requestedQuantity", request.getRequestedQuantity());
        snapshot.put("unit", request.getUnit());
        snapshot.put("chefNote", request.getChefNote());
        snapshot.put("requestType", request.getRequestType());
        snapshot.put("status", request.getStatus() != null ? request.getStatus().name() : null);

        return snapshot;
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
        snapshot.put("maxStock", item.getMaxStock());
        snapshot.put("branchId", item.getBranch() != null ? item.getBranch().getId() : null);
        snapshot.put("branchName", item.getBranch() != null ? item.getBranch().getName() : null);

        return snapshot;
    }
}