package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByUser(User user);

    Optional<Staff> findByUserId(Long userId);

    /*
     * Loads Staff together with Branch.
     * This is needed in JwtAuthenticationFilter because branch is lazy-loaded.
     */
    @Query("SELECT s FROM Staff s LEFT JOIN FETCH s.branch WHERE s.user.id = :userId")
    Optional<Staff> findByUserIdWithBranch(@Param("userId") Long userId);

    /*
     * Optimized staff list query.
     * Loads Staff + User + Role + Branch in one query.
     * This avoids userRepository.findAll() + repeated staffRepository.findByUserId().
     */
    @Query("""
            SELECT DISTINCT s
            FROM Staff s
            JOIN FETCH s.user u
            JOIN FETCH u.role r
            LEFT JOIN FETCH s.branch b
            ORDER BY u.id ASC
            """)
    List<Staff> findAllStaffWithUserRoleBranch();

    /*
     * Optimized branch staff query.
     */
    @Query("""
            SELECT DISTINCT s
            FROM Staff s
            JOIN FETCH s.user u
            JOIN FETCH u.role r
            LEFT JOIN FETCH s.branch b
            WHERE b.id = :branchId
            ORDER BY u.id ASC
            """)
    List<Staff> findByBranchIdWithUserRoleBranch(@Param("branchId") Long branchId);

    /*
     * Optimized role staff query.
     */
    @Query("""
            SELECT DISTINCT s
            FROM Staff s
            JOIN FETCH s.user u
            JOIN FETCH u.role r
            LEFT JOIN FETCH s.branch b
            WHERE UPPER(r.name) = UPPER(:roleName)
            ORDER BY u.id ASC
            """)
    List<Staff> findByRoleNameWithUserRoleBranch(@Param("roleName") String roleName);

    /*
     * Optimized branch + role staff query.
     * Used when ADMIN filters by role inside own branch.
     */
    @Query("""
            SELECT DISTINCT s
            FROM Staff s
            JOIN FETCH s.user u
            JOIN FETCH u.role r
            LEFT JOIN FETCH s.branch b
            WHERE b.id = :branchId
            AND UPPER(r.name) = UPPER(:roleName)
            ORDER BY u.id ASC
            """)
    List<Staff> findByBranchIdAndRoleNameWithUserRoleBranch(
            @Param("branchId") Long branchId,
            @Param("roleName") String roleName
    );

    long countByBranchIdAndUserRoleNameAndEmploymentStatus(
            Long branchId,
            String roleName,
            com.ByteKnights.com.resturarent_system.entity.EmploymentStatus status
    );

    long countByBranchIdAndUserRoleNameInAndEmploymentStatus(
            Long branchId,
            java.util.Collection<String> roleNames,
            com.ByteKnights.com.resturarent_system.entity.EmploymentStatus status
    );

    long countByBranchIdAndUserRoleName(Long branchId, String roleName);

    List<Staff> findByBranchIdAndUserRoleName(Long branchId, String roleName);

    List<Staff> findByBranchId(Long branchId);

    boolean existsByUser(User user);

    // --- Kitchen Queries START ---

    @Query("SELECT s FROM Staff s WHERE s.branch.id = :branchId " +
            "AND s.user.role.name = 'LINE_CHEF' " +
            "AND s.employmentStatus = 'ACTIVE' " +
            "AND s.user.isActive = true " +
            "AND s.id NOT IN (SELECT ca.staff.id FROM ChefAttendance ca WHERE ca.attendanceDate = :date)")
    List<Staff> findLineChefsNotCheckedInToday(
            @Param("branchId") Long branchId,
            @Param("date") LocalDate date
    );

    @Query("SELECT s FROM Staff s WHERE s.branch.id = :branchId AND s.user.role.name = 'LINE_CHEF' AND s.employmentStatus = 'ACTIVE'")
    List<Staff> findAllActiveLineChefsByBranch(@Param("branchId") Long branchId);

    @Query("SELECT COUNT(s) FROM Staff s WHERE s.branch.id = :branchId AND s.user.role.name = 'LINE_CHEF' AND s.employmentStatus = 'ACTIVE'")
    long countActiveLineChefsByBranch(@Param("branchId") Long branchId);

    // --- Kitchen Queries END ---
}