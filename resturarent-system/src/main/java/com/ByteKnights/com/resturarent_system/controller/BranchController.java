package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.BranchResponse;
import com.ByteKnights.com.resturarent_system.dto.CreateBranchRequest;
import com.ByteKnights.com.resturarent_system.dto.UpdateBranchRequest;
import com.ByteKnights.com.resturarent_system.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public BranchResponse createBranch(@RequestBody CreateBranchRequest request) {
        return branchService.createBranch(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<BranchResponse> getAllBranches() {
        return branchService.getAllBranches();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public BranchResponse getBranchById(@PathVariable Long id) {
        return branchService.getBranchById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public BranchResponse updateBranch(@PathVariable Long id,
                                       @RequestBody UpdateBranchRequest request) {
        return branchService.updateBranch(id, request);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public BranchResponse activateBranch(@PathVariable Long id) {
        return branchService.activateBranch(id);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public BranchResponse deactivateBranch(@PathVariable Long id) {
        return branchService.deactivateBranch(id);
    }
}