package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.AvailableChefDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.ChefDashboardStatsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.ChefDetailsDTO;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.ChefAttendanceRepository;
import com.ByteKnights.com.resturarent_system.repository.OrderItemRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.service.KitchenChefService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KitchenChefServiceImpl implements KitchenChefService {

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final ChefAttendanceRepository chefAttendanceRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public ChefDashboardStatsDTO getChefDashboardStats(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Long branchId = staff.getBranch().getId();
        LocalDate today = LocalDate.now();

        long total = staffRepository.countActiveLineChefsByBranch(branchId);
        long onDuty = chefAttendanceRepository.countChefsByAttendanceStatusToday(today, branchId, ChefAttendanceStatus.ON_DUTY);
        long offDuty = chefAttendanceRepository.countChefsByAttendanceStatusToday(today, branchId, ChefAttendanceStatus.OFF_DUTY);
        long available = chefAttendanceRepository.countChefsByWorkStatusToday(today, branchId, ChefWorkStatus.AVAILABLE);

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

            Optional<ChefAttendance> attendance = chefAttendanceRepository.findByStaffIdAndAttendanceDate(chef.getId(), LocalDate.now());

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
                List.of(ChefWorkStatus.AVAILABLE)
        );

        return records.stream()
                .map(ca -> new AvailableChefDTO(
                        ca.getStaff().getId(),
                        ca.getStaff().getUser().getFullName(),
                        ca.getWorkStatus().name()
                ))
                .toList();
    }

    @Override
    @Transactional
    public void checkInChef(Long chefId) {
        Optional<ChefAttendance> existingRecord = chefAttendanceRepository.findByStaffIdAndAttendanceDate(chefId, LocalDate.now());

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

        chefAttendanceRepository.save(attendance);
    }

    @Override
    @Transactional
    public void checkOutChef(Long chefId) {
        ChefAttendance attendance = chefAttendanceRepository.findByStaffIdAndAttendanceDate(chefId, LocalDate.now())
                .orElseThrow(() -> new RuntimeException("No check-in record found for this chef today!"));

        if (attendance.getAttendanceStatus() == ChefAttendanceStatus.OFF_DUTY) {
            throw new RuntimeException("Chef has already checked out for today!");
        }

        if (attendance.getWorkStatus() == ChefWorkStatus.COOKING) {
            throw new RuntimeException("Cannot check-out while a meal is in preparation!");
        }

        attendance.setClockOutTime(LocalDateTime.now());
        attendance.setAttendanceStatus(ChefAttendanceStatus.OFF_DUTY);
        attendance.setWorkStatus(ChefWorkStatus.UNAVAILABLE);

        chefAttendanceRepository.save(attendance);
    }

    @Override
    @Transactional
    public void updateChefWorkStatus(Long chefId, ChefWorkStatus newStatus) {
        ChefAttendance attendance = chefAttendanceRepository.findByStaffIdAndAttendanceDate(chefId, LocalDate.now())
                .orElseThrow(() -> new RuntimeException("Chef is not checked in today!. Cannot update status."));

        if (attendance.getAttendanceStatus() == ChefAttendanceStatus.OFF_DUTY) {
            throw new RuntimeException("Cannot update status! Chef has already checked out for today.");
        }

        if (newStatus == ChefWorkStatus.UNAVAILABLE) {
            throw new RuntimeException("Cannot set status to UNAVAILABLE manually! Use Check-out instead.");
        }

        attendance.setWorkStatus(newStatus);
        chefAttendanceRepository.save(attendance);
    }
}
