package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.ApiResponse;
import com.ByteKnights.com.resturarent_system.dto.request.customer.CustomerLoginRequest;
import com.ByteKnights.com.resturarent_system.dto.request.customer.CustomerOtpRequest;
import com.ByteKnights.com.resturarent_system.dto.request.customer.CustomerOtpVerifyRequest;
import com.ByteKnights.com.resturarent_system.dto.request.customer.CustomerRegisterRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CustomerLoginResponseData;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CustomerRegisterResponseData;
import com.ByteKnights.com.resturarent_system.service.CustomerAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/customer")
@CrossOrigin
public class CustomerAuthController {

    private final CustomerAuthService customerAuthService;

    public CustomerAuthController(CustomerAuthService customerAuthService) {
        this.customerAuthService = customerAuthService;
    }

    //online customer regiter controller
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<CustomerRegisterResponseData>> register(
            @RequestBody CustomerRegisterRequest request) {
        CustomerRegisterResponseData responseData = customerAuthService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Customer registered successfully.", responseData));
    }

    //online customer login controller
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<CustomerLoginResponseData>> login(
            @RequestBody CustomerLoginRequest request) {
        CustomerLoginResponseData responseData = customerAuthService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful.", responseData));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Object>> requestOtp(@RequestBody CustomerOtpRequest request) {
        customerAuthService.requestOtp(request.getPhone());
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully.", null));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<CustomerLoginResponseData>> verifyOtp(@RequestBody CustomerOtpVerifyRequest request) {
        CustomerLoginResponseData responseData = customerAuthService.verifyOtp(request.getPhone(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success("Phone verified successfully.", responseData));
    }
}
