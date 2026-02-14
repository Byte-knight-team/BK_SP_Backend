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
        return ResponseEntity.ok("GET /api/menu — not yet implemented");
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMenuItemById(@PathVariable Long id) {
        // TODO: Return single menu item by ID
        return ResponseEntity.ok("GET /api/menu/" + id + " — not yet implemented");
    }

    @PostMapping
    public ResponseEntity<?> createMenuItem(/* TODO: @RequestBody MenuItemDto dto */) {
        // TODO: Create menu item (admin/manager only)
        return ResponseEntity.ok("POST /api/menu — not yet implemented");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMenuItem(@PathVariable Long id /* TODO: @RequestBody MenuItemDto dto */) {
        // TODO: Update menu item (admin/manager only)
        return ResponseEntity.ok("PUT /api/menu/" + id + " — not yet implemented");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMenuItem(@PathVariable Long id) {
        // TODO: Delete menu item (admin/manager only)
        return ResponseEntity.ok("DELETE /api/menu/" + id + " — not yet implemented");
    }
}
