package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.superadmin.SuperAdminCustomerResponse;

import java.util.List;

public interface SuperAdminCustomerService {

    List<SuperAdminCustomerResponse> getAllCustomers();

    SuperAdminCustomerResponse getCustomerById(Long customerId);

    SuperAdminCustomerResponse activateCustomer(Long customerId);

    SuperAdminCustomerResponse deactivateCustomer(Long customerId);
}