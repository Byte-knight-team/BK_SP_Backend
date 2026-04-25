package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.admin.CreateMenuCategoryRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.UpdateMenuCategoryRequest;
import com.ByteKnights.com.resturarent_system.dto.response.admin.MenuCategoryResponse;

public interface MenuCategoryService {

    MenuCategoryResponse createCategory(CreateMenuCategoryRequest request);

    MenuCategoryResponse updateCategory(Long id, UpdateMenuCategoryRequest request);

    MenuCategoryResponse deleteCategory(Long id);
}
