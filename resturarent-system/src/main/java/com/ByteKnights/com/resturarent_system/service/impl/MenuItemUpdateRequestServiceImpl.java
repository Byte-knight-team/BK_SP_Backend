package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.admin.MenuItemUpdateDecisionDto;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.MenuItemUpdateRequestDto;
import com.ByteKnights.com.resturarent_system.dto.response.admin.MenuItemUpdateRequestResponseDto;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.MenuItemRepository;
import com.ByteKnights.com.resturarent_system.repository.MenuItemUpdateRequestRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.MenuItemUpdateRequestService;
import com.ByteKnights.com.resturarent_system.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuItemUpdateRequestServiceImpl implements MenuItemUpdateRequestService {

    private final MenuItemUpdateRequestRepository repository;
    private final StaffRepository staffRepository;
    private final MenuItemRepository menuItemRepository;
    private final WebSocketNotificationService webSocketNotificationService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public void createRequest(Long chefId, MenuItemUpdateRequestDto requestDto) {
        Staff chef = staffRepository.findById(chefId)
                .orElseThrow(() -> new RuntimeException("Chef not found with ID: " + chefId));

        MenuItem menuItem = menuItemRepository.findById(requestDto.getMenuItemId())
                .orElseThrow(() -> new RuntimeException("Menu item not found with ID: " + requestDto.getMenuItemId()));

        if (!chef.getBranch().getId().equals(menuItem.getBranch().getId())) {
            throw new RuntimeException("Chef can only create requests for menu items in their own branch.");
        }

        MenuItemUpdateRequest request = MenuItemUpdateRequest.builder()
                .chef(chef)
                .menuItem(menuItem)
                .chefNote(requestDto.getChefNote())
                .status(MenuItemUpdateRequestStatus.PENDING)
                .build();

        MenuItemUpdateRequest savedRequest = repository.save(request);

        auditLogService.logCurrentUserAction(
                AuditModule.MENU,
                AuditEventType.MENU_ITEM_UPDATE_REQUEST_CREATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.MENU_ITEM_UPDATE_REQUEST,
                savedRequest.getId(),
                getMenuItemBranchId(menuItem),
                "Menu item update request created by chef",
                null,
                buildRequestAuditSnapshot(savedRequest)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemUpdateRequestResponseDto> getAllRequests(MenuItemUpdateRequestStatus status) {
        List<MenuItemUpdateRequest> requests;

        if (status != null) {
            requests = repository.findByStatus(status);
        } else {
            requests = repository.findAll();
        }

        return requests.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateRequestDecision(Long requestId, MenuItemUpdateDecisionDto decisionDto) {
        MenuItemUpdateRequest request = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with ID: " + requestId));

        Map<String, Object> oldValues = buildRequestAuditSnapshot(request);

        request.setStatus(decisionDto.getStatus());
        request.setAdminNote(decisionDto.getAdminNote());

        MenuItemUpdateRequest savedRequest = repository.save(request);

        auditLogService.logCurrentUserAction(
                AuditModule.MENU,
                AuditEventType.MENU_ITEM_UPDATE_REQUEST_STATUS_UPDATED,
                AuditStatus.SUCCESS,
                decisionDto.getStatus() == MenuItemUpdateRequestStatus.APPROVED
                        ? AuditSeverity.INFO
                        : AuditSeverity.WARN,
                AuditTargetType.MENU_ITEM_UPDATE_REQUEST,
                savedRequest.getId(),
                getMenuItemBranchId(savedRequest.getMenuItem()),
                "Menu item update request decision updated by admin",
                oldValues,
                buildRequestAuditSnapshot(savedRequest)
        );

        if (decisionDto.getStatus() == MenuItemUpdateRequestStatus.APPROVED
                && request.getChef() != null
                && request.getChef().getUser() != null) {

            String adminName = decisionDto.getAdminName() != null ? decisionDto.getAdminName() : "Admin";
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String message = String.format(
                    "Menu item updated request approved - %s by %s on %s",
                    request.getMenuItem().getName(),
                    adminName,
                    dateStr
            );

            webSocketNotificationService.broadcastMenuUpdateApprovalToChef(
                    request.getChef().getUser().getId(),
                    message
            );
        }
    }

    private MenuItemUpdateRequestResponseDto mapToDto(MenuItemUpdateRequest request) {
        Staff chef = request.getChef();
        MenuItem item = request.getMenuItem();

        MenuCategory category = item.getCategory();
        String subCategory = item.getSubCategory();

        String chefName = "Unknown Chef";

        if (chef != null && chef.getUser() != null && chef.getUser().getFullName() != null) {
            chefName = chef.getUser().getFullName();
        } else if (chef != null) {
            chefName = (chef.getFirstName() != null ? chef.getFirstName() : "")
                    + (chef.getLastName() != null ? " " + chef.getLastName() : "");

            if (chefName.trim().isEmpty()) {
                chefName = "Unknown Chef";
            }
        }

        return MenuItemUpdateRequestResponseDto.builder()
                .id(request.getId())
                .chefId(chef.getId())
                .chefName(chefName.trim())
                .menuItemId(item.getId())
                .menuItemName(item.getName())
                .menuCategory(category != null ? category.getName() : null)
                .menuSubCategory(subCategory)
                .menuItemImage(item.getImageUrl())
                .chefNote(request.getChefNote())
                .adminNote(request.getAdminNote())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .build();
    }

    private Map<String, Object> buildRequestAuditSnapshot(MenuItemUpdateRequest request) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (request == null) {
            return snapshot;
        }

        Staff chef = request.getChef();
        MenuItem item = request.getMenuItem();

        snapshot.put("requestId", request.getId());
        snapshot.put("status", request.getStatus() != null ? request.getStatus().name() : null);
        snapshot.put("chefNote", request.getChefNote());
        snapshot.put("adminNote", request.getAdminNote());
        snapshot.put("createdAt", request.getCreatedAt());

        snapshot.put("chefId", chef != null ? chef.getId() : null);
        snapshot.put("chefName", buildStaffName(chef));

        snapshot.put("menuItemId", item != null ? item.getId() : null);
        snapshot.put("menuItemName", item != null ? item.getName() : null);
        snapshot.put("branchId", getMenuItemBranchId(item));

        return snapshot;
    }

    private Long getMenuItemBranchId(MenuItem item) {
        if (item == null || item.getBranch() == null) {
            return null;
        }

        return item.getBranch().getId();
    }

    private String buildStaffName(Staff staff) {
        if (staff == null) {
            return null;
        }

        if (staff.getUser() != null && staff.getUser().getFullName() != null) {
            return staff.getUser().getFullName();
        }

        String firstName = staff.getFirstName() != null ? staff.getFirstName() : "";
        String lastName = staff.getLastName() != null ? staff.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();

        return fullName.isBlank() ? null : fullName;
    }
}