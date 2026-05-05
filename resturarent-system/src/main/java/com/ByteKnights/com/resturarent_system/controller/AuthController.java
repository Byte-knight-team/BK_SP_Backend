package com.ByteKnights.com.resturarent_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ByteKnights.com.resturarent_system.auth.AuthService;
import com.ByteKnights.com.resturarent_system.dto.ChangePasswordRequest;
import com.ByteKnights.com.resturarent_system.dto.LoginResponse;
import com.ByteKnights.com.resturarent_system.dto.StaffLoginRequest;

/*
 * AuthController handles authentication-related API requests.
 * It receives requests from the frontend and passes the actual logic to AuthService.
 */

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;



    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/staff/login")
    public ResponseEntity<LoginResponse> staffLogin(@RequestBody StaffLoginRequest request) {
        LoginResponse response = authService.loginStaff(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/staff/change-password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest request) {
        String result = authService.changePassword(request);
        return ResponseEntity.ok(result);
    }
}