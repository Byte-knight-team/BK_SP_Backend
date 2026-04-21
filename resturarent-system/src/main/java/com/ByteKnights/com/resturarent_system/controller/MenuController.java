package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.ApiResponse;
import com.ByteKnights.com.resturarent_system.dto.response.customer.MenuItemResponse;
import com.ByteKnights.com.resturarent_system.service.MenuService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/menu")
@CrossOrigin
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    //CUSTOMER ENDPOINT (Only active items)
    @GetMapping("/customer")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> getMenu(
            @RequestParam(required = false) Long branchId) {
        
        List<MenuItemResponse> menuItems = menuService.fetchCustomerMenu(branchId);
        
        return ResponseEntity.ok(ApiResponse.success("Menu fetched successfully", menuItems));
    }
}