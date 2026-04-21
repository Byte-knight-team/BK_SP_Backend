package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.customer.MenuItemResponse;
import java.util.List;

public interface MenuService {
    List<MenuItemResponse> fetchCustomerMenu(Long branchId);
}