package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import com.ByteKnights.com.resturarent_system.repository.ChefRequestRepository;
import com.ByteKnights.com.resturarent_system.repository.InventoryItemRepository;
import com.ByteKnights.com.resturarent_system.service.InventoryService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * The actual implementation of the InventoryService.
 * 
 * The @Service annotation tells Spring Boot that this class holds business
 * logic.
 * Spring will automatically register this as a "Bean" and inject it wherever
 * InventoryService is requested (like in your Controller).
 */
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    /**
     * INJECTED REPOSITORIES
     * 
     * Because of the @RequiredArgsConstructor annotation on the class,
     * Lombok generates a constructor that requires these three repositories.
     * Spring Boot then automatically "injects" the live database connections into
     * these variables at runtime.
     */
    private final InventoryItemRepository inventoryItemRepository;
    private final ChefRequestRepository chefRequestRepository;
    private final BranchRepository branchRepository;

}
