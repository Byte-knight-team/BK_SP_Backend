package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Privilege;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrivilegeRepository extends JpaRepository<Privilege, Long> {

    Optional<Privilege> findByName(String name);

    // Useful for frontend privilege dropdown/list
    List<Privilege> findAllByOrderByNameAsc();
}