package com.ByteKnights.com.resturarent_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    // TODO: Inject MenuItemService

    @GetMapping
    public ResponseEntity<?> getAllMenuItems() {
        // TODO: Return list of menu items (public)
        return null;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMenuItemById(@PathVariable Long id) {
        // TODO: Return single menu item by ID
        return null;
    }

    @PostMapping
    public ResponseEntity<?> createMenuItem(/* TODO: @RequestBody MenuItemDto dto */) {
        // TODO: Create menu item (admin/manager only)
        return null;
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMenuItem(@PathVariable Long id /* TODO: @RequestBody MenuItemDto dto */) {
        // TODO: Update menu item (admin/manager only)
        return null;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMenuItem(@PathVariable Long id) {
        // TODO: Delete menu item (admin/manager only)
        return null;
    }
}
