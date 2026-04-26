package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByUser(User user);

    Optional<Staff> findByUserId(Long userId);

    /*
     * Loads Staff together with Branch.
     * This is needed in JwtAuthenticationFilter because branch is lazy-loaded.
     * Without JOIN FETCH, staff.getBranch().getStatus() can cause LazyInitializationException.
     */
    @Query("SELECT s FROM Staff s LEFT JOIN FETCH s.branch WHERE s.user.id = :userId")
    Optional<Staff> findByUserIdWithBranch(@Param("userId") Long userId);

    boolean existsByUser(User user);
}