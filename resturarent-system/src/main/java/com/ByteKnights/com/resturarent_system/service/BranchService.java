package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.BranchResponse;
import com.ByteKnights.com.resturarent_system.dto.CreateBranchRequest;
import com.ByteKnights.com.resturarent_system.dto.UpdateBranchRequest;
import com.ByteKnights.com.resturarent_system.entity.AuditEventType;
import com.ByteKnights.com.resturarent_system.entity.AuditModule;
import com.ByteKnights.com.resturarent_system.entity.AuditSeverity;
import com.ByteKnights.com.resturarent_system.entity.AuditStatus;
import com.ByteKnights.com.resturarent_system.entity.AuditTargetType;
import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.entity.BranchStatus;
import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ByteKnights.com.resturarent_system.audit.Auditable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final AuditLogService auditLogService;

    //@Auditable annotation is used to log the audit trail
    @Auditable(module = AuditModule.BRANCH, eventType = AuditEventType.BRANCH_CREATED, targetType = AuditTargetType.BRANCH, description = "Branch created successfully", captureResultAsNewValue = false)
    //transaction annotation is used to rollback the transaction if any error occurs
    @Transactional
    //createBranch method is used to create a new branch
    public BranchResponse createBranch(CreateBranchRequest request) {
        StringBuilder validationErrors = new StringBuilder();

        if (isBlank(request.getName())) {
            validationErrors.append("Branch name is required. ");
        }

        if (isBlank(request.getAddress())) {
            validationErrors.append("Address is required. ");
        }

        if (isBlank(request.getContactNumber())) {
            validationErrors.append("Contact number is required. ");
        } 
        else if (!isValidContactNumber(request.getContactNumber())) {
            validationErrors.append("Contact number is invalid. ");
        }

        if (!isBlank(request.getEmail()) && !isValidEmail(request.getEmail())) {
            validationErrors.append("Invalid email format. ");
        }

        if (validationErrors.length() > 0) {
            return BranchResponse.builder()
                    .message(validationErrors.toString().trim())
                    .build();
        }

        String trimmedName = request.getName().trim();

        if (branchRepository.existsByNameIgnoreCase(trimmedName)) {
            return BranchResponse.builder()
                    .message("Branch name already exists")
                    .build();
        }

        //create branch
        Branch branch = Branch.builder()
                .name(trimmedName)
                .address(request.getAddress().trim())
                .contactNumber(request.getContactNumber().trim())
                .email(trimToNull(request.getEmail()))
                .status(BranchStatus.ACTIVE)
                .build();

        //save branch
        Branch savedBranch = branchRepository.save(branch);

        //This returns a branch response with the success message
        return mapToResponse(savedBranch, "Branch created successfully");
    }


    @Transactional(readOnly = true) 
    //get all branches
    public List<BranchResponse> getAllBranches() {
        return branchRepository.findAll()
                .stream()
                //converts the branch entity to a branch response
                .map(branch -> mapToResponse(branch, null)) 
                //collects the branch responses into a list
                .collect(Collectors.toList()); 
    }

    @Transactional(readOnly = true)
    //get branch by id
    public BranchResponse getBranchById(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        return mapToResponse(branch, null);
    }

    @Transactional
    //update branch
    public BranchResponse updateBranch(Long id, UpdateBranchRequest request) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        //build branch audit snapshot before updating the branch
        Map<String, Object> oldValues = buildBranchAuditSnapshot(branch);

        if (request.getName() != null) {
            String newName = request.getName().trim();

            if (newName.isEmpty()) {
                throw new RuntimeException("Branch name cannot be empty");
            }

            if (!branch.getName().equalsIgnoreCase(newName)
                    && branchRepository.existsByNameIgnoreCase(newName)) {
                throw new RuntimeException("Branch name already exists");
            }

            //update branch name
            branch.setName(newName);
        }

        if (request.getAddress() != null) {
            String newAddress = request.getAddress().trim();

            if (newAddress.isEmpty()) {
                throw new RuntimeException("Address cannot be empty");
            }

            //update branch address
            branch.setAddress(newAddress);
        }

        if (request.getContactNumber() != null) {
            String newContactNumber = request.getContactNumber().trim();

            if (newContactNumber.isEmpty()) {
                throw new RuntimeException("Contact number cannot be empty");
            }

            if (!isValidContactNumber(newContactNumber)) {
                throw new RuntimeException("Contact number is invalid");
            }

            //update branch contact number
            branch.setContactNumber(newContactNumber);
        }

        if (request.getEmail() != null) {
            String newEmail = trimToNull(request.getEmail());

            if (newEmail != null && !isValidEmail(newEmail)) {
                throw new RuntimeException("Invalid email format");
            }

            //update branch email
            branch.setEmail(newEmail);
        }

        //save the updated branch
        Branch updatedBranch = branchRepository.save(branch);

        //this logs the audit trail of the updated branch
        auditLogService.logCurrentUserAction(
                AuditModule.BRANCH,
                AuditEventType.BRANCH_UPDATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.BRANCH,
                updatedBranch.getId(),
                updatedBranch.getId(),
                "Branch updated successfully",
                oldValues,
                buildBranchAuditSnapshot(updatedBranch));

        return mapToResponse(updatedBranch, "Branch updated successfully");
    }

    @Auditable(module = AuditModule.BRANCH, eventType = AuditEventType.BRANCH_ACTIVATED, targetType = AuditTargetType.BRANCH, description = "Branch activated successfully", captureResultAsNewValue = false)
    @Transactional
    public BranchResponse activateBranch(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        
        branch.setStatus(BranchStatus.ACTIVE);

        Branch updatedBranch = branchRepository.save(branch);

        return mapToResponse(updatedBranch, "Branch activated successfully");
    }

    @Auditable(module = AuditModule.BRANCH, eventType = AuditEventType.BRANCH_DEACTIVATED, targetType = AuditTargetType.BRANCH, description = "Branch deactivated successfully", captureResultAsNewValue = false)
    @Transactional
    public BranchResponse deactivateBranch(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        branch.setStatus(BranchStatus.INACTIVE);

        Branch updatedBranch = branchRepository.save(branch);

        return mapToResponse(updatedBranch, "Branch deactivated successfully");
    }

    //this method is used to map a branch entity to a branch response
    private BranchResponse mapToResponse(Branch branch, String message) {
        return BranchResponse.builder()
                .id(branch.getId())
                .name(branch.getName())
                .address(branch.getAddress())
                .contactNumber(branch.getContactNumber())
                .email(branch.getEmail())
                .status(branch.getStatus().name())
                .createdAt(branch.getCreatedAt())
                .message(message)
                .build();
    }

    //this method is used to build a branch audit snapshot
    private Map<String, Object> buildBranchAuditSnapshot(Branch branch) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("branchId", branch.getId());
        snapshot.put("name", branch.getName());
        snapshot.put("address", branch.getAddress());
        snapshot.put("contactNumber", branch.getContactNumber());
        snapshot.put("email", branch.getEmail());
        snapshot.put("status", branch.getStatus() != null ? branch.getStatus().name() : null);
        snapshot.put("createdAt", branch.getCreatedAt());
        return snapshot;
    }

    //this method is used to check if a string is blank
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    //this method is used to trim a string to null
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    //this method is used to validate an email
    private boolean isValidEmail(String email) {
        //Using regex to validate email
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    //this method is used to validate a contact number
    private boolean isValidContactNumber(String contactNumber) {
        //Using regex to validate contact number
        return contactNumber.matches("^[+0-9\\-\\s]{7,20}$");
    }
}