package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.customer.CustomerPasswordUpdateRequest;
import com.ByteKnights.com.resturarent_system.dto.request.customer.CustomerProfileUpdateRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CustomerProfileResponse;

public interface CustomerProfileService {
    CustomerProfileResponse getCustomerProfile(String email);
    CustomerProfileResponse updateCustomerProfile(String currentEmail, CustomerProfileUpdateRequest request);
    void updatePassword(String email, CustomerPasswordUpdateRequest request);
}