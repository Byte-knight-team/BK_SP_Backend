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

    @Override
    public ManagerStaffSummaryDTO getStaffSummary(Long targetBranchId, Long userId) {
        Long finalBranchId = resolveBranchId(targetBranchId, userId);
        String branchName = branchRepository.findById(finalBranchId)
                .map(b -> b.getName())
                .orElse("Unknown Branch");

        // 1. Fetch all staff members for this branch
        List<Staff> staffList = staffRepository.findByBranchId(finalBranchId);

        // 2. Map members to DTOs
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        List<ManagerStaffMemberDTO> memberDTOs = staffList.stream()
                .map(s -> ManagerStaffMemberDTO.builder()
                        .userId(s.getUser() != null ? s.getUser().getId() : null)
                        .name(s.getFirstName() + " " + (s.getLastName() != null ? s.getLastName() : ""))
                        .role(s.getUser() != null && s.getUser().getRole() != null ? formatRole(s.getUser().getRole().getName()) : "Unknown")
                        .hireDate(s.getHireDate() != null ? s.getHireDate().format(dateFormatter) : "N/A")
                        .contactNumber(s.getUser() != null ? s.getUser().getPhone() : "N/A")
                        .salary(s.getSalary())
                        .status(s.getEmploymentStatus() != null ? s.getEmploymentStatus().name() : "ACTIVE")
                        .build())
                .collect(Collectors.toList());

        // 3. Aggregate counts for summary cards (ACTIVE only as per design "Active Kitchen Staff")
        int kitchenCount = (int) staffRepository.countByBranchIdAndUserRoleNameAndEmploymentStatus(
                finalBranchId, "CHEF", EmploymentStatus.ACTIVE);
        
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

    private String formatRole(String roleName) {
        if (roleName == null) return "Unknown";
        switch (roleName.toUpperCase()) {
            case "CHEF": return "Kitchen Staff";
            case "DELIVERY": return "Delivery Driver";
            case "RECEPTIONIST": return "Receptionist";
            case "MANAGER": return "Manager";
            default: return roleName;
        }
    }

    private Long resolveBranchId(Long targetBranchId, Long userId) {
        if (targetBranchId != null) return targetBranchId;
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not assigned to any branch as staff"));
        return (staff.getBranch() != null) ? staff.getBranch().getId() : null;
    }
}
