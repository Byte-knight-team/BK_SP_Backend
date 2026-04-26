package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.ChefAttendance;
import com.ByteKnights.com.resturarent_system.entity.ChefAttendanceStatus;
import com.ByteKnights.com.resturarent_system.entity.ChefWorkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ChefAttendanceRepository extends JpaRepository<ChefAttendance, Long> {

    // --- Kitchen Queries START ---

    // 1. find all available line chefs in a branch to assign for meals
    // this is JPQL (Java Persistence Query Language - uses Entity classes and fields instead of table names)
    // for clean, object-oriented queries that involve relationships (joins) between entities.
    @Query("SELECT ca FROM ChefAttendance ca " +
            "WHERE ca.attendanceDate = :date " +
            "AND ca.staff.branch.id = :branchId " +
            "AND ca.attendanceStatus = :attendanceStatus " +
            "AND ca.workStatus IN :workStatuses " +
            "AND ca.staff.user.role.name = 'LINE_CHEF' " +
            "AND ca.staff.employmentStatus = 'ACTIVE' " +
            "AND ca.staff.user.isActive = true")
    List<ChefAttendance> findAvailableLineChefsForBranch(
            @Param("date") LocalDate date,
            @Param("branchId") Long branchId,
            @Param("attendanceStatus") ChefAttendanceStatus attendanceStatus,
            @Param("workStatuses") List<ChefWorkStatus> workStatuses
    );

    // --- Kitchen Queries END ---
}
