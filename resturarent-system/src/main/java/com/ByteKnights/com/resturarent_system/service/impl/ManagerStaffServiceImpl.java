package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.manager.ManagerStaffMemberDTO;
import com.ByteKnights.com.resturarent_system.dto.response.manager.ManagerStaffSummaryDTO;
import com.ByteKnights.com.resturarent_system.entity.EmploymentStatus;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.service.ManagerStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerStaffServiceImpl implements ManagerStaffService {

    private final StaffRepository staffRepository;
    private final BranchRepository branchRepository;

    /**
     * Retrieves a summary of all active staff members assigned to the Manager's branch.
     * Excludes higher-level administrative roles from the list.
     * 
     * @param targetBranchId Optional branch ID filter.
     * @param userId Authenticated manager's user ID.
     * @return DTO containing aggregate counts and a list of staff member details.
     */
    @Override
    public ManagerStaffSummaryDTO getStaffSummary(Long targetBranchId, Long userId) {
        Long finalBranchId = resolveBranchId(targetBranchId, userId);
        String branchName = branchRepository.findById(finalBranchId)
                .map(b -> b.getName())
                .orElse("Unknown Branch");

        // 1. Fetch all staff members for this branch
        List<Staff> staffList = staffRepository.findByBranchId(finalBranchId);

        // 2. Map members to DTOs (Exclude Managers and Admins)
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        List<ManagerStaffMemberDTO> memberDTOs = staffList.stream()
                .filter(s -> s.getUser() != null && s.getUser().getRole() != null)
                .filter(s -> {
                    String role = s.getUser().getRole().getName().toUpperCase();
                    return !role.equals("MANAGER") && !role.equals("ADMIN") && !role.equals("SUPER_ADMIN");
                })
                .map(s -> ManagerStaffMemberDTO.builder()
                        .userId(s.getUser().getId())
                        .name(s.getUser().getUsername())
                        .role(formatRole(s.getUser().getRole().getName()))
                        .hireDate(s.getHireDate() != null ? s.getHireDate().format(dateFormatter) : "N/A")
                        .contactNumber(s.getUser().getPhone())
                        .salary(s.getSalary())
                        .status(s.getEmploymentStatus() != null ? s.getEmploymentStatus().name() : "ACTIVE")
                        .build())
                .collect(Collectors.toList());

        // 3. Aggregate counts for summary cards (ACTIVE only as per design "Active Kitchen Staff")
        int kitchenCount = (int) staffRepository.countByBranchIdAndUserRoleNameInAndEmploymentStatus(
                finalBranchId, java.util.Arrays.asList("CHEF", "LINE_CHEF"), EmploymentStatus.ACTIVE);
        
        int deliveryCount = (int) staffRepository.countByBranchIdAndUserRoleNameAndEmploymentStatus(
                finalBranchId, "DELIVERY", EmploymentStatus.ACTIVE);
        
        int receptionistCount = (int) staffRepository.countByBranchIdAndUserRoleNameAndEmploymentStatus(
                finalBranchId, "RECEPTIONIST", EmploymentStatus.ACTIVE);

        return ManagerStaffSummaryDTO.builder()
                .branchName(branchName)
                .kitchenCount(kitchenCount)
                .deliveryCount(deliveryCount)
                .receptionistCount(receptionistCount)
                .staffMembers(memberDTOs)
                .build();
    }

    /**
     * Formats raw database role names into user-friendly display labels.
     */
    private String formatRole(String roleName) {
        if (roleName == null) return "Unknown";
        switch (roleName.toUpperCase()) {
            case "CHEF":
            case "LINE_CHEF": return "Kitchen Staff";
            case "DELIVERY": return "Delivery Driver";
            case "RECEPTIONIST": return "Receptionist";
            case "MANAGER": return "Manager";
            default: return roleName;
        }
    }

    /**
     * Helper method to determine the correct branch context.
     */
    private Long resolveBranchId(Long targetBranchId, Long userId) {
        if (targetBranchId != null) return targetBranchId;
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not assigned to any branch as staff"));
        return (staff.getBranch() != null) ? staff.getBranch().getId() : null;
    }
}
