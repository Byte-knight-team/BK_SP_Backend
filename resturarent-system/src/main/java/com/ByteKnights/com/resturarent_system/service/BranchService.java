package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.audit.Auditable;
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
import com.ByteKnights.com.resturarent_system.entity.SystemConfig;
import com.ByteKnights.com.resturarent_system.repository.SystemConfigRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchService {

        private final BranchRepository branchRepository;
        private final AuditLogService auditLogService;
        private final SystemConfigRepository systemConfigRepository;

        /*
         * Creates a new branch.
         *
         * New branches must include a map-selected latitude and longitude.
         */
        @Auditable(module = AuditModule.BRANCH, eventType = AuditEventType.BRANCH_CREATED, targetType = AuditTargetType.BRANCH, description = "Branch created successfully", captureResultAsNewValue = false)
        @Transactional
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
                } else if (!isValidContactNumber(request.getContactNumber())) {
                        validationErrors.append("Contact number is invalid. ");
                }

                if (!isBlank(request.getEmail())
                                && !isValidEmail(request.getEmail())) {
                        validationErrors.append("Invalid email format. ");
                }

                /*
                 * A new branch must have a complete map location.
                 */
                if (request.getLatitude() == null
                                || request.getLongitude() == null) {
                        validationErrors.append(
                                        "Please select the branch location from the map. ");
                } else {
                        if (!isValidLatitude(request.getLatitude())) {
                                validationErrors.append(
                                                "Latitude must be between -90 and 90. ");
                        }

                        if (!isValidLongitude(request.getLongitude())) {
                                validationErrors.append(
                                                "Longitude must be between -180 and 180. ");
                        }
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

                Branch branch = Branch.builder()
                                .name(trimmedName)
                                .address(request.getAddress().trim())
                                .contactNumber(request.getContactNumber().trim())
                                .email(trimToNull(request.getEmail()))
                                .latitude(request.getLatitude())
                                .longitude(request.getLongitude())
                                .status(BranchStatus.ACTIVE)
                                .build();

                Branch savedBranch = branchRepository.save(branch);

                return mapToResponse(
                                savedBranch,
                                "Branch created successfully");
        }

        /*
         * Returns all branches, including active and inactive branches.
         */
        @Transactional(readOnly = true)
        public List<BranchResponse> getAllBranches() {
                return branchRepository.findAll()
                                .stream()
                                .map(branch -> mapToResponse(branch, null))
                                .collect(Collectors.toList());
        }

        /*
         * Returns one branch by ID.
         */
        @Transactional(readOnly = true)
        public BranchResponse getBranchById(Long id) {

                Branch branch = branchRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Branch not found"));

                return mapToResponse(branch, null);
        }

        /*
         * Updates branch information.
         *
         * Latitude and longitude are updated only when both values are supplied.
         * This preserves compatibility with older requests that update only the
         * original branch fields.
         */
        @Transactional
        public BranchResponse updateBranch(
                        Long id,
                        UpdateBranchRequest request) {

                Branch branch = branchRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Branch not found"));

                Map<String, Object> oldValues = buildBranchAuditSnapshot(branch);

                if (request.getName() != null) {
                        String newName = request.getName().trim();

                        if (newName.isEmpty()) {
                                throw new RuntimeException(
                                                "Branch name cannot be empty");
                        }

                        if (!branch.getName().equalsIgnoreCase(newName)
                                        && branchRepository.existsByNameIgnoreCase(newName)) {
                                throw new RuntimeException(
                                                "Branch name already exists");
                        }

                        branch.setName(newName);
                }

                if (request.getAddress() != null) {
                        String newAddress = request.getAddress().trim();

                        if (newAddress.isEmpty()) {
                                throw new RuntimeException(
                                                "Address cannot be empty");
                        }

                        branch.setAddress(newAddress);
                }

                if (request.getContactNumber() != null) {
                        String newContactNumber = request.getContactNumber().trim();

                        if (newContactNumber.isEmpty()) {
                                throw new RuntimeException(
                                                "Contact number cannot be empty");
                        }

                        if (!isValidContactNumber(newContactNumber)) {
                                throw new RuntimeException(
                                                "Contact number is invalid");
                        }

                        branch.setContactNumber(newContactNumber);
                }

                if (request.getEmail() != null) {
                        String newEmail = trimToNull(request.getEmail());

                        if (newEmail != null && !isValidEmail(newEmail)) {
                                throw new RuntimeException(
                                                "Invalid email format");
                        }

                        branch.setEmail(newEmail);
                }

                boolean latitudeProvided = request.getLatitude() != null;
                boolean longitudeProvided = request.getLongitude() != null;

                /*
                 * Coordinates must always be provided as a complete pair.
                 */
                if (latitudeProvided != longitudeProvided) {
                        throw new IllegalArgumentException(
                                        "Latitude and longitude must be provided together");
                }

                if (latitudeProvided) {
                        if (!isValidLatitude(request.getLatitude())) {
                                throw new IllegalArgumentException(
                                                "Latitude must be between -90 and 90");
                        }

                        if (!isValidLongitude(request.getLongitude())) {
                                throw new IllegalArgumentException(
                                                "Longitude must be between -180 and 180");
                        }

                        branch.setLatitude(request.getLatitude());
                        branch.setLongitude(request.getLongitude());
                }

                Branch updatedBranch = branchRepository.save(branch);

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

                return mapToResponse(
                                updatedBranch,
                                "Branch updated successfully");
        }

        /*
         * Activates a branch.
         */
        @Auditable(module = AuditModule.BRANCH, eventType = AuditEventType.BRANCH_ACTIVATED, targetType = AuditTargetType.BRANCH, description = "Branch activated successfully", captureResultAsNewValue = false)
        @Transactional
        public BranchResponse activateBranch(Long id) {

                Branch branch = branchRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Branch not found"));

                branch.setStatus(BranchStatus.ACTIVE);

                Branch updatedBranch = branchRepository.save(branch);

                return mapToResponse(
                                updatedBranch,
                                "Branch activated successfully");
        }

        /*
         * Deactivates a branch.
         *
         * Delivery-branch protection will be added with the SystemConfig batch.
         */
        @Auditable(module = AuditModule.BRANCH, eventType = AuditEventType.BRANCH_DEACTIVATED, targetType = AuditTargetType.BRANCH, description = "Branch deactivated successfully", captureResultAsNewValue = false)
        @Transactional
        public BranchResponse deactivateBranch(Long id) {

                Branch branch = branchRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Branch not found"));

                boolean selectedDeliveryBranch = systemConfigRepository
                                .findFirstByOrderByIdAsc()
                                .map(SystemConfig::getDeliveryBranch)
                                .map(Branch::getId)
                                .filter(id::equals)
                                .isPresent();

                if (selectedDeliveryBranch) {
                        throw new IllegalArgumentException(
                                        "This branch is currently configured as the delivery branch. "
                                                        + "Select another delivery branch before deactivating it.");
                }

                branch.setStatus(BranchStatus.INACTIVE);

                Branch updatedBranch = branchRepository.save(branch);

                return mapToResponse(
                                updatedBranch,
                                "Branch deactivated successfully");
        }

        /*
         * Maps Branch entity data into the existing API response.
         */
        private BranchResponse mapToResponse(
                        Branch branch,
                        String message) {

                return BranchResponse.builder()
                                .id(branch.getId())
                                .name(branch.getName())
                                .address(branch.getAddress())
                                .contactNumber(branch.getContactNumber())
                                .email(branch.getEmail())
                                .status(
                                                branch.getStatus() != null
                                                                ? branch.getStatus().name()
                                                                : null)
                                .latitude(branch.getLatitude())
                                .longitude(branch.getLongitude())
                                .createdAt(branch.getCreatedAt())
                                .message(message)
                                .build();
        }

        /*
         * Builds safe old/new values for branch audit logging.
         */
        private Map<String, Object> buildBranchAuditSnapshot(
                        Branch branch) {

                Map<String, Object> snapshot = new LinkedHashMap<>();

                if (branch == null) {
                        return snapshot;
                }

                snapshot.put("branchId", branch.getId());
                snapshot.put("name", branch.getName());
                snapshot.put("address", branch.getAddress());
                snapshot.put("contactNumber", branch.getContactNumber());
                snapshot.put("email", branch.getEmail());
                snapshot.put("latitude", branch.getLatitude());
                snapshot.put("longitude", branch.getLongitude());

                snapshot.put(
                                "status",
                                branch.getStatus() != null
                                                ? branch.getStatus().name()
                                                : null);

                snapshot.put("createdAt", branch.getCreatedAt());

                return snapshot;
        }

        private boolean isBlank(String value) {
                return value == null || value.trim().isEmpty();
        }

        private String trimToNull(String value) {
                if (value == null) {
                        return null;
                }

                String trimmed = value.trim();

                return trimmed.isEmpty() ? null : trimmed;
        }

        private boolean isValidEmail(String email) {
                return email.matches(
                                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
        }

        private boolean isValidContactNumber(String contactNumber) {
                return contactNumber.matches(
                                "^[+0-9\\-\\s]{7,20}$");
        }

        private boolean isValidLatitude(Double latitude) {
                return latitude != null
                                && Double.isFinite(latitude)
                                && latitude >= -90
                                && latitude <= 90;
        }

        private boolean isValidLongitude(Double longitude) {
                return longitude != null
                                && Double.isFinite(longitude)
                                && longitude >= -180
                                && longitude <= 180;
        }
}