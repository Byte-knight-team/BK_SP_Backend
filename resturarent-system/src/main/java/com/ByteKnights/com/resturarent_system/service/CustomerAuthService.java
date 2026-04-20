package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.CustomerLoginRequest;
import com.ByteKnights.com.resturarent_system.dto.request.CustomerRegisterRequest;
import com.ByteKnights.com.resturarent_system.dto.response.CustomerLoginResponseData;
import com.ByteKnights.com.resturarent_system.dto.response.CustomerRegisterResponseData;

public interface CustomerAuthService {
    CustomerRegisterResponseData register(CustomerRegisterRequest request);

    CustomerLoginResponseData login(CustomerLoginRequest request);
}
