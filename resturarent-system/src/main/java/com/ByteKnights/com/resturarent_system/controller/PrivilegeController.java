package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.entity.Privilege;
import com.ByteKnights.com.resturarent_system.repository.PrivilegeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/privileges")
public class PrivilegeController {

    private final PrivilegeRepository privilegeRepository;

    public PrivilegeController(PrivilegeRepository privilegeRepository) {
        this.privilegeRepository = privilegeRepository;
    }

    // SUPER_ADMIN and ADMIN can view all privileges
    // Frontend can use this to build role permission checkboxes/toggles
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<List<Privilege>> getAllPrivileges() {
        return ResponseEntity.ok(privilegeRepository.findAllByOrderByNameAsc());
    }
}