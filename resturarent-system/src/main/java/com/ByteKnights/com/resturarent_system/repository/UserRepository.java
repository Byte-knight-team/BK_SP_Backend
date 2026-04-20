package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Role;
import com.ByteKnights.com.resturarent_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByUsername(String username);

    // Count only active users for a given role
    long countByRoleAndIsActiveTrue(Role role);

    // Count all users for a given role
    long countByRole(Role role);

    // Used before deleting a role
    boolean existsByRole(Role role);
}