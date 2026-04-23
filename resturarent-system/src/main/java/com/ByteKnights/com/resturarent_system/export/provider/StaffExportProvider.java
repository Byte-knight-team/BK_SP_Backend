package com.ByteKnights.com.resturarent_system.export.provider;

import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.export.ExportFormat;
import com.ByteKnights.com.resturarent_system.export.ExportTarget;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StaffExportProvider implements ExportDataProvider {

    private static final Set<String> STAFF_ROLES = Set.of(
            "SUPER_ADMIN",
            "ADMIN",
            "MANAGER",
            "CHEF",
            "RECEPTIONIST",
            "DELIVERY"
    );

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;

    @Override
    public ExportTarget getTarget() {
        return ExportTarget.STAFF;
    }

    @Override
    public String getBaseFileName() {
        return "staff";
    }

    @Override
    public Set<ExportFormat> getSupportedFormats() {
        return EnumSet.of(ExportFormat.CSV, ExportFormat.JSON);
    }

    @Override
    public List<LinkedHashMap<String, Object>> getCsvRows() {
        return userRepository.findAll(Sort.by(Sort.Order.asc("id"))).stream()
                .filter(this::isStaffUser)
                .map(this::toCsvRow)
                .collect(Collectors.toList());
    }

    @Override
    public Object getJsonData() {
        return userRepository.findAll(Sort.by(Sort.Order.asc("id"))).stream()
                .filter(this::isStaffUser)
                .map(this::toJsonRow)
                .collect(Collectors.toList());
    }

    private LinkedHashMap<String, Object> toCsvRow(User user) {
        Staff staff = staffRepository.findByUserId(user.getId()).orElse(null);

        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("userId", user.getId());
        row.put("fullName", user.getFullName());
        row.put("username", user.getUsername());
        row.put("email", user.getEmail());
        row.put("phone", user.getPhone());
        row.put("address", user.getAddress());
        row.put("roleName", user.getRole() != null ? user.getRole().getName() : null);
        row.put("active", user.getIsActive());
        row.put("passwordChanged", user.getPasswordChanged());
        row.put("inviteStatus", user.getInviteStatus() != null ? user.getInviteStatus().name() : null);
        row.put("emailSent", user.getEmailSent());
        row.put("lastInviteAttemptAt", user.getLastInviteAttemptAt());
        row.put("createdAt", user.getCreatedAt());
        row.put("updatedAt", user.getUpdatedAt());

        row.put("staffId", staff != null ? staff.getId() : null);
        row.put("branchId", staff != null && staff.getBranch() != null ? staff.getBranch().getId() : null);
        row.put("branchName", staff != null && staff.getBranch() != null ? staff.getBranch().getName() : null);
        row.put("firstName", staff != null ? staff.getFirstName() : null);
        row.put("lastName", staff != null ? staff.getLastName() : null);
        row.put("nic", staff != null ? staff.getNic() : null);
        row.put("salary", staff != null ? staff.getSalary() : null);
        row.put("hireDate", staff != null ? staff.getHireDate() : null);
        row.put("performanceRating", staff != null ? staff.getPerformanceRating() : null);
        row.put("employmentStatus", staff != null && staff.getEmploymentStatus() != null
                ? staff.getEmploymentStatus().name()
                : null);
        row.put("staffCreatedAt", staff != null ? staff.getCreatedAt() : null);

        return row;
    }

    private LinkedHashMap<String, Object> toJsonRow(User user) {
        Staff staff = staffRepository.findByUserId(user.getId()).orElse(null);

        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("userId", user.getId());
        row.put("fullName", user.getFullName());
        row.put("username", user.getUsername());
        row.put("email", user.getEmail());
        row.put("phone", user.getPhone());
        row.put("address", user.getAddress());
        row.put("roleName", user.getRole() != null ? user.getRole().getName() : null);
        row.put("active", user.getIsActive());
        row.put("passwordChanged", user.getPasswordChanged());
        row.put("inviteStatus", user.getInviteStatus() != null ? user.getInviteStatus().name() : null);
        row.put("emailSent", user.getEmailSent());
        row.put("lastInviteAttemptAt", user.getLastInviteAttemptAt());
        row.put("createdAt", user.getCreatedAt());
        row.put("updatedAt", user.getUpdatedAt());

        LinkedHashMap<String, Object> staffData = new LinkedHashMap<>();
        staffData.put("staffId", staff != null ? staff.getId() : null);
        staffData.put("branchId", staff != null && staff.getBranch() != null ? staff.getBranch().getId() : null);
        staffData.put("branchName", staff != null && staff.getBranch() != null ? staff.getBranch().getName() : null);
        staffData.put("firstName", staff != null ? staff.getFirstName() : null);
        staffData.put("lastName", staff != null ? staff.getLastName() : null);
        staffData.put("nic", staff != null ? staff.getNic() : null);
        staffData.put("salary", staff != null ? staff.getSalary() : null);
        staffData.put("hireDate", staff != null ? staff.getHireDate() : null);
        staffData.put("performanceRating", staff != null ? staff.getPerformanceRating() : null);
        staffData.put("employmentStatus", staff != null && staff.getEmploymentStatus() != null
                ? staff.getEmploymentStatus().name()
                : null);
        staffData.put("staffCreatedAt", staff != null ? staff.getCreatedAt() : null);

        row.put("staff", staffData);

        return row;
    }

    private boolean isStaffUser(User user) {
        if (user == null || user.getRole() == null || user.getRole().getName() == null) {
            return false;
        }

        return STAFF_ROLES.contains(user.getRole().getName().trim().toUpperCase());
    }
}