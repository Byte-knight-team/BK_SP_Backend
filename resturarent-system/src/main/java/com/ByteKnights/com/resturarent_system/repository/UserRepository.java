package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.InventoryItem;
import com.ByteKnights.com.resturarent_system.entity.Role;
import com.ByteKnights.com.resturarent_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByUsername(String username);

    long countByRoleAndIsActiveTrue(Role role);

    long countByRole(Role role);

    boolean existsByRole(Role role);

    @Query("SELECT u.id FROM User u JOIN u.role r WHERE UPPER(r.name) LIKE CONCAT('%', UPPER(:roleKeyword), '%')")
    List<Long> findUserIdsByRoleKeyword(@Param("roleKeyword") String roleKeyword);

    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    long countActiveUsers();

    @Query("SELECT COUNT(s) FROM Staff s JOIN s.user u WHERE s.branch.id = :branchId AND u.isActive = true")
    long countActiveUsersByBranchId(@Param("branchId") Long branchId);

}
