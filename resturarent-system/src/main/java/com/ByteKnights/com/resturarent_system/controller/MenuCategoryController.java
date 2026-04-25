package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.request.admin.CreateMenuCategoryRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.UpdateMenuCategoryRequest;
import com.ByteKnights.com.resturarent_system.dto.response.admin.MenuCategoryResponse;
import com.ByteKnights.com.resturarent_system.service.MenuCategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class MenuCategoryController {

    private final MenuCategoryService menuCategoryService;
    private final com.ByteKnights.com.resturarent_system.service.MenuService menuService;

    public MenuCategoryController(MenuCategoryService menuCategoryService, com.ByteKnights.com.resturarent_system.service.MenuService menuService) {
        this.menuCategoryService = menuCategoryService;
        this.menuService = menuService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<MenuCategoryResponse>> getAllCategories() {
        List<MenuCategoryResponse> categories = menuCategoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<MenuCategoryResponse> getCategoryById(@PathVariable Long id) {
        MenuCategoryResponse category = menuCategoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Long> getCategoriesCount() {
        long count = menuService.getCategoryCount();
        return ResponseEntity.ok(count);
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<MenuCategoryResponse> createCategory(@RequestBody CreateMenuCategoryRequest request) {
        MenuCategoryResponse created = menuCategoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<MenuCategoryResponse> updateCategory(
            @PathVariable Long id,
            @RequestBody UpdateMenuCategoryRequest request) {
        MenuCategoryResponse updated = menuCategoryService.updateCategory(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<MenuCategoryResponse> deleteCategory(@PathVariable Long id) {
        MenuCategoryResponse deleted = menuCategoryService.deleteCategory(id);
        return ResponseEntity.ok(deleted);
    }
}
