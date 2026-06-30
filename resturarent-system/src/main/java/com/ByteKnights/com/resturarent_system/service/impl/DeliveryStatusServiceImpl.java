package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.entity.AuditEventType;
import com.ByteKnights.com.resturarent_system.entity.AuditModule;
import com.ByteKnights.com.resturarent_system.entity.AuditSeverity;
import com.ByteKnights.com.resturarent_system.entity.AuditStatus;
import com.ByteKnights.com.resturarent_system.entity.AuditTargetType;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.DeliveryStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeliveryStatusServiceImpl implements DeliveryStatusService {

    private final StaffRepository staffRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public void toggleOnlineStatus(Long userId, boolean online) {
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Staff member not found for user ID: " + userId
                ));

        /*
         * Manual audit is used because this changes delivery staff online status.
         * oldValuesJson shows previous online status.
         * newValuesJson shows updated online status.
         */
        Map<String, Object> oldValues = buildDeliveryStaffAuditSnapshot(staff);

        staff.setIsOnline(online);

        Staff savedStaff = staffRepository.save(staff);

        auditLogService.logCurrentUserAction(
                AuditModule.DELIVERY,
                AuditEventType.DELIVERY_ONLINE_STATUS_UPDATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.DELIVERY,
                savedStaff.getId(),
                getStaffBranchId(savedStaff),
                online
                        ? "Delivery staff marked online successfully"
                        : "Delivery staff marked offline successfully",
                oldValues,
                buildDeliveryStaffAuditSnapshot(savedStaff)
        );
    }

    @Override
    public boolean getOnlineStatus(Long userId) {
        return staffRepository.findByUserId(userId)
                .map(Staff::isOnline)
                .orElse(false);
    }

    /*
     * Builds safe audit JSON for delivery staff online/offline status.
     */
    private Map<String, Object> buildDeliveryStaffAuditSnapshot(Staff staff) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (staff == null) {
            return snapshot;
        }

        snapshot.put("staffId", staff.getId());
        snapshot.put("userId", staff.getUser() != null ? staff.getUser().getId() : null);
        snapshot.put("fullName", staff.getUser() != null ? staff.getUser().getFullName() : null);
        snapshot.put("email", staff.getUser() != null ? staff.getUser().getEmail() : null);

        snapshot.put("branchId", staff.getBranch() != null ? staff.getBranch().getId() : null);
        snapshot.put("branchName", staff.getBranch() != null ? staff.getBranch().getName() : null);

        snapshot.put("online", staff.isOnline());

        return snapshot;
    }

    /*
     * Gets staff branch ID for audit branch filtering.
     */
    private Long getStaffBranchId(Staff staff) {
        if (staff == null || staff.getBranch() == null) {
            return null;
        }

        return staff.getBranch().getId();
    }
}