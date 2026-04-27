package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.customer.CustomerLoginRequest;
import com.ByteKnights.com.resturarent_system.dto.request.customer.CustomerRegisterRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CustomerLoginResponseData;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CustomerRegisterResponseData;

public interface CustomerAuthService {
    CustomerRegisterResponseData register(CustomerRegisterRequest request);

    CustomerLoginResponseData login(CustomerLoginRequest request);

    CustomerLoginResponseData verifyOtp(String phone, String code, Long sessionId);

    public void requestOtp(String phone);
}
