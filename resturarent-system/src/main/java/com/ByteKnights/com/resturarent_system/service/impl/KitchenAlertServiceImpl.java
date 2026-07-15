package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.audit.Auditable;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.CreateAlertRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.ActiveAlertDTO;
import com.ByteKnights.com.resturarent_system.entity.AuditEventType;
import com.ByteKnights.com.resturarent_system.entity.AuditModule;
import com.ByteKnights.com.resturarent_system.entity.AuditSeverity;
import com.ByteKnights.com.resturarent_system.entity.AuditStatus;
import com.ByteKnights.com.resturarent_system.entity.AuditTargetType;
import com.ByteKnights.com.resturarent_system.entity.KitchenAlert;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.repository.KitchenAlertRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.KitchenAlertService;
import com.ByteKnights.com.resturarent_system.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KitchenAlertServiceImpl implements KitchenAlertService {

    private final KitchenAlertRepository kitchenAlertRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final WebSocketNotificationService webSocketNotificationService;
    private final AuditLogService auditLogService;

    @Override
    @Auditable(
            module = AuditModule.KITCHEN,
            eventType = AuditEventType.KITCHEN_ALERT_CREATED,
            targetType = AuditTargetType.KITCHEN_ALERT,
            successSeverity = AuditSeverity.WARN,
            description = "Kitchen alert created successfully",
            captureResultAsNewValue = false
    )
    @Transactional
    public void createKitchenAlert(CreateAlertRequestDTO dto, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        KitchenAlert alert = KitchenAlert.builder()
                .branch(staff.getBranch())
                .reportedBy(staff)
                .message(dto.getMessage())
                .type(dto.getType())
                .isResolved(false)
                .build();

        KitchenAlert savedAlert = kitchenAlertRepository.save(alert);

        ActiveAlertDTO alertDTO = new ActiveAlertDTO(
                savedAlert.getId(),
                savedAlert.getMessage(),
                savedAlert.getType(),
                "just now",
                false,
                null
        );

        webSocketNotificationService.broadcastKitchenAlert(staff.getBranch().getId(), alertDTO);
    }

    @Override
    public List<ActiveAlertDTO> getActiveAlerts(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        List<KitchenAlert> alerts = kitchenAlertRepository
                .findByBranchIdAndIsResolvedFalseOrderByCreatedAtDesc(staff.getBranch().getId());

        List<ActiveAlertDTO> activeAlertDTOs = new ArrayList<>();

        for (KitchenAlert alert : alerts) {
            long minutes = Duration.between(alert.getCreatedAt(), LocalDateTime.now()).toMinutes();
            String timeAgo = minutes < 60 ? minutes + "m" : (minutes / 60) + "h";

            ActiveAlertDTO dto = new ActiveAlertDTO(
                    alert.getId(),
                    alert.getMessage(),
                    alert.getType(),
                    timeAgo,
                    false,
                    null
            );

            activeAlertDTOs.add(dto);
        }

        return activeAlertDTOs;
    }

    @Override
    @Transactional
    public void resolveAlert(Long alertId, String userEmail) {
        KitchenAlert alert = kitchenAlertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));

        if (alert.isResolved()) {
            throw new RuntimeException("This alert has already been resolved");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        /*
         * Branch safety check:
         * Staff should resolve only alerts that belong to their own branch.
         */
        Long staffBranchId = staff.getBranch().getId();
        Long alertBranchId = alert.getBranch() != null ? alert.getBranch().getId() : null;

        if (alertBranchId == null || !alertBranchId.equals(staffBranchId)) {
            throw new RuntimeException("You cannot resolve an alert from another branch");
        }

        /*
         * Manual audit is required because we need old/new resolved status,
         * resolvedBy, and resolvedAt values.
         */
        Map<String, Object> oldValues = buildKitchenAlertAuditSnapshot(alert);

        alert.setResolved(true);
        alert.setResolvedBy(staff);
        alert.setResolvedAt(LocalDateTime.now());

        KitchenAlert savedAlert = kitchenAlertRepository.save(alert);

        auditLogService.logCurrentUserAction(
                AuditModule.KITCHEN,
                AuditEventType.KITCHEN_ALERT_RESOLVED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.KITCHEN_ALERT,
                savedAlert.getId(),
                staffBranchId,
                "Kitchen alert resolved successfully",
                oldValues,
                buildKitchenAlertAuditSnapshot(savedAlert)
        );

        // Notify the branch (receptionist) that the issue is cleared.
        webSocketNotificationService.broadcastKitchenAlertResolved(staffBranchId, savedAlert.getMessage());
    }

    /*
     * Builds a safe audit snapshot for kitchen alerts.
     */
    private Map<String, Object> buildKitchenAlertAuditSnapshot(KitchenAlert alert) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (alert == null) {
            return snapshot;
        }

        Staff reportedBy = alert.getReportedBy();
        Staff resolvedBy = alert.getResolvedBy();

        snapshot.put("alertId", alert.getId());
        snapshot.put("branchId", alert.getBranch() != null ? alert.getBranch().getId() : null);
        snapshot.put("branchName", alert.getBranch() != null ? alert.getBranch().getName() : null);
        snapshot.put("message", alert.getMessage());
        snapshot.put("type", alert.getType());
        snapshot.put("resolved", alert.isResolved());
        snapshot.put("createdAt", alert.getCreatedAt());
        snapshot.put("resolvedAt", alert.getResolvedAt());

        snapshot.put("reportedByStaffId", reportedBy != null ? reportedBy.getId() : null);
        snapshot.put("reportedByUserId",
                reportedBy != null && reportedBy.getUser() != null ? reportedBy.getUser().getId() : null);
        snapshot.put("reportedByName",
                reportedBy != null && reportedBy.getUser() != null ? reportedBy.getUser().getFullName() : null);

        snapshot.put("resolvedByStaffId", resolvedBy != null ? resolvedBy.getId() : null);
        snapshot.put("resolvedByUserId",
                resolvedBy != null && resolvedBy.getUser() != null ? resolvedBy.getUser().getId() : null);
        snapshot.put("resolvedByName",
                resolvedBy != null && resolvedBy.getUser() != null ? resolvedBy.getUser().getFullName() : null);

        return snapshot;
    }
}