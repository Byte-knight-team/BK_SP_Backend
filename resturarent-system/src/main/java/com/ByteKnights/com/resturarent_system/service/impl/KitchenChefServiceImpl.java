package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.AvailableChefDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.ChefDashboardStatsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.ChefDetailsDTO;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.ChefAttendanceRepository;
import com.ByteKnights.com.resturarent_system.repository.OrderItemRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.KitchenChefService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KitchenChefServiceImpl implements KitchenChefService {

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final ChefAttendanceRepository chefAttendanceRepository;
    private final OrderItemRepository orderItemRepository;
    private final AuditLogService auditLogService;

    @Override
    public ChefDashboardStatsDTO getChefDashboardStats(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Long branchId = staff.getBranch().getId();
        LocalDate today = LocalDate.now();

        long total = staffRepository.countActiveLineChefsByBranch(branchId);
        long onDuty = chefAttendanceRepository.countChefsByAttendanceStatusToday(
                today,
                branchId,
                ChefAttendanceStatus.ON_DUTY
        );
        long offDuty = chefAttendanceRepository.countChefsByAttendanceStatusToday(
                today,
                branchId,
                ChefAttendanceStatus.OFF_DUTY
        );
        long available = chefAttendanceRepository.countChefsByWorkStatusToday(
                today,
                branchId,
                ChefWorkStatus.AVAILABLE
        );

        return new ChefDashboardStatsDTO(total, onDuty, offDuty, available);
    }

    @Override
    public List<ChefDetailsDTO> getChefDetailsToday(String chiefChefEmail) {
        User chiefChef = userRepository.findByEmail(chiefChefEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff currentStaff = staffRepository.findByUser(chiefChef)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Long branchId = currentStaff.getBranch().getId();

        List<Staff> lineChefs = staffRepository.findAllActiveLineChefsByBranch(branchId);
        List<ChefDetailsDTO> dtoList = new ArrayList<>();

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        for (Staff chef : lineChefs) {
            if (chef.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
                continue;
            }

            Optional<ChefAttendance> attendance = chefAttendanceRepository.findByStaffIdAndAttendanceDate(
                    chef.getId(),
                    LocalDate.now()
            );

            String clockIn = (attendance.isPresent() && attendance.get().getClockInTime() != null)
                    ? attendance.get().getClockInTime().format(timeFormatter)
                    : "---";

            String clockOut = (attendance.isPresent() && attendance.get().getClockOutTime() != null)
                    ? attendance.get().getClockOutTime().format(timeFormatter)
                    : "---";

            String status = attendance.isPresent()
                    ? attendance.get().getWorkStatus().toString()
                    : "UNAVAILABLE";

            long mealsToday = orderItemRepository.countMealsPreparedToday(chef.getId(), startOfToday);

            dtoList.add(new ChefDetailsDTO(
                    chef.getId(),
                    chef.getUser().getFullName(),
                    clockIn,
                    clockOut,
                    status,
                    mealsToday
            ));
        }

        return dtoList;
    }

    @Override
    public List<AvailableChefDTO> getAvailableChefs(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Long branchId = staff.getBranch().getId();

        List<ChefAttendance> records = chefAttendanceRepository.findAvailableLineChefsForBranch(
                LocalDate.now(),
                branchId,
                ChefAttendanceStatus.ON_DUTY,
                List.of(ChefWorkStatus.AVAILABLE, ChefWorkStatus.COOKING)
        );

        return records.stream()
                .map(ca -> new AvailableChefDTO(
                        ca.getStaff().getId(),
                        ca.getStaff().getUser().getFullName(),
                        ca.getWorkStatus().name(),
                        orderItemRepository.countActiveItemsByLineChef(ca.getStaff().getId())
                ))
                .toList();
    }

    @Override
    @Transactional
    public void checkInChef(Long chefId) {
        Optional<ChefAttendance> existingRecord = chefAttendanceRepository.findByStaffIdAndAttendanceDate(
                chefId,
                LocalDate.now()
        );

        if (existingRecord.isPresent()) {
            ChefAttendance record = existingRecord.get();

            if (record.getAttendanceStatus() == ChefAttendanceStatus.ON_DUTY) {
                throw new RuntimeException("Chef is already currently clocked in!");
            }

            if (record.getAttendanceStatus() == ChefAttendanceStatus.OFF_DUTY) {
                throw new RuntimeException("Cannot check-in for today. Chef has already completed their shift for today!");
            }
        }

        Staff chef = staffRepository.findById(chefId)
                .orElseThrow(() -> new RuntimeException("Chef not found"));

        ChefAttendance attendance = new ChefAttendance();
        attendance.setStaff(chef);
        attendance.setAttendanceDate(LocalDate.now());
        attendance.setClockInTime(LocalDateTime.now());
        attendance.setAttendanceStatus(ChefAttendanceStatus.ON_DUTY);
        attendance.setWorkStatus(ChefWorkStatus.AVAILABLE);

        ChefAttendance savedAttendance = chefAttendanceRepository.save(attendance);

        /*
         * Manual audit is used because this method returns void.
         * We save the created attendance record as newValuesJson.
         */
        auditLogService.logCurrentUserAction(
                AuditModule.KITCHEN,
                AuditEventType.CHEF_CHECKED_IN,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.USER,
                getChefUserId(chef),
                getChefBranchId(chef),
                "Chef checked in successfully",
                null,
                buildChefAttendanceAuditSnapshot(savedAttendance)
        );
    }

    @Override
    @Transactional
    public void checkOutChef(Long chefId) {
        ChefAttendance attendance = chefAttendanceRepository.findByStaffIdAndAttendanceDate(
                        chefId,
                        LocalDate.now()
                )
                .orElseThrow(() -> new RuntimeException("No check-in record found for this chef today!"));

        if (attendance.getAttendanceStatus() == ChefAttendanceStatus.OFF_DUTY) {
            throw new RuntimeException("Chef has already checked out for today!");
        }

        if (attendance.getWorkStatus() == ChefWorkStatus.COOKING) {
            throw new RuntimeException("Cannot check-out while a meal is in preparation!");
        }

        /*
         * Capture old values before changing clock-out and attendance status.
         */
        Map<String, Object> oldValues = buildChefAttendanceAuditSnapshot(attendance);

        attendance.setClockOutTime(LocalDateTime.now());
        attendance.setAttendanceStatus(ChefAttendanceStatus.OFF_DUTY);
        attendance.setWorkStatus(ChefWorkStatus.UNAVAILABLE);

        ChefAttendance savedAttendance = chefAttendanceRepository.save(attendance);

        auditLogService.logCurrentUserAction(
                AuditModule.KITCHEN,
                AuditEventType.CHEF_CHECKED_OUT,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.USER,
                getChefUserId(savedAttendance.getStaff()),
                getChefBranchId(savedAttendance.getStaff()),
                "Chef checked out successfully",
                oldValues,
                buildChefAttendanceAuditSnapshot(savedAttendance)
        );
    }

    @Override
    @Transactional
    public void updateChefWorkStatus(Long chefId, ChefWorkStatus newStatus) {
        ChefAttendance attendance = chefAttendanceRepository.findByStaffIdAndAttendanceDate(
                        chefId,
                        LocalDate.now()
                )
                .orElseThrow(() -> new RuntimeException("Chef is not checked in today!. Cannot update status."));

        if (attendance.getAttendanceStatus() == ChefAttendanceStatus.OFF_DUTY) {
            throw new RuntimeException("Cannot update status! Chef has already checked out for today.");
        }

        if (newStatus == ChefWorkStatus.UNAVAILABLE) {
            throw new RuntimeException("Cannot set status to UNAVAILABLE manually! Use Check-out instead.");
        }

        /*
         * Manual audit is required because this is a status update.
         * oldValuesJson shows previous work status and newValuesJson shows updated work status.
         */
        Map<String, Object> oldValues = buildChefAttendanceAuditSnapshot(attendance);

        attendance.setWorkStatus(newStatus);

        ChefAttendance savedAttendance = chefAttendanceRepository.save(attendance);

        auditLogService.logCurrentUserAction(
                AuditModule.KITCHEN,
                AuditEventType.CHEF_WORK_STATUS_UPDATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.USER,
                getChefUserId(savedAttendance.getStaff()),
                getChefBranchId(savedAttendance.getStaff()),
                "Chef work status updated successfully",
                oldValues,
                buildChefAttendanceAuditSnapshot(savedAttendance)
        );
    }

    /*
     * Builds a safe audit snapshot for chef attendance.
     *
     * We do not store the full entity directly because Staff -> User -> Role relationships
     * can create large JSON or lazy-loading issues.
     */
    private Map<String, Object> buildChefAttendanceAuditSnapshot(ChefAttendance attendance) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (attendance == null) {
            return snapshot;
        }

        Staff staff = attendance.getStaff();
        User user = staff != null ? staff.getUser() : null;

        snapshot.put("attendanceId", attendance.getId());
        snapshot.put("attendanceDate", attendance.getAttendanceDate());
        snapshot.put("clockInTime", attendance.getClockInTime());
        snapshot.put("clockOutTime", attendance.getClockOutTime());

        snapshot.put("attendanceStatus",
                attendance.getAttendanceStatus() != null
                        ? attendance.getAttendanceStatus().name()
                        : null);

        snapshot.put("workStatus",
                attendance.getWorkStatus() != null
                        ? attendance.getWorkStatus().name()
                        : null);

        snapshot.put("chefStaffId", staff != null ? staff.getId() : null);
        snapshot.put("chefUserId", user != null ? user.getId() : null);
        snapshot.put("chefName", user != null ? user.getFullName() : null);
        snapshot.put("chefEmail", user != null ? user.getEmail() : null);

        snapshot.put("branchId",
                staff != null && staff.getBranch() != null
                        ? staff.getBranch().getId()
                        : null);

        snapshot.put("branchName",
                staff != null && staff.getBranch() != null
                        ? staff.getBranch().getName()
                        : null);

        return snapshot;
    }

    /*
     * Returns the User ID of the chef.
     * AuditTargetType.USER is used because the current enum has no CHEF or CHEF_ATTENDANCE target type.
     */
    private Long getChefUserId(Staff chef) {
        if (chef == null || chef.getUser() == null) {
            return null;
        }

        return chef.getUser().getId();
    }

    /*
     * Returns the branch ID of the chef for audit branch filtering.
     */
    private Long getChefBranchId(Staff chef) {
        if (chef == null || chef.getBranch() == null) {
            return null;
        }

        return chef.getBranch().getId();
    }
}