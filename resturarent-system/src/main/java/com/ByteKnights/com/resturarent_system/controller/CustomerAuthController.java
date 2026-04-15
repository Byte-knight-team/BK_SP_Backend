package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.ApiResponse;
import com.ByteKnights.com.resturarent_system.dto.CustomerLoginRequest;
import com.ByteKnights.com.resturarent_system.dto.CustomerLoginResponseData;
import com.ByteKnights.com.resturarent_system.dto.CustomerRegisterRequest;
import com.ByteKnights.com.resturarent_system.dto.CustomerRegisterResponseData;
import com.ByteKnights.com.resturarent_system.exception.CustomerAuthException;
import com.ByteKnights.com.resturarent_system.service.CustomerAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/customer")
public class CustomerAuthController {

    private final CustomerAuthService customerAuthService;

    public CustomerAuthController(CustomerAuthService customerAuthService) {
        this.customerAuthService = customerAuthService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<CustomerRegisterResponseData>> register(
            @RequestBody(required = false) CustomerRegisterRequest request) {
        try {
            CustomerRegisterResponseData responseData = customerAuthService.register(request);
            return ResponseEntity.ok(ApiResponse.success("Customer registered successfully.", responseData));
        } catch (CustomerAuthException ex) {
            return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<CustomerLoginResponseData>> login(
            @RequestBody(required = false) CustomerLoginRequest request) {
        try {
            CustomerLoginResponseData responseData = customerAuthService.login(request);
            return ResponseEntity.ok(ApiResponse.success("Login successful.", responseData));
        } catch (CustomerAuthException ex) {
            return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error"));
        }
    }
}
