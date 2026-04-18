package com.ByteKnights.com.resturarent_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ByteKnights.com.resturarent_system.auth.AuthService;
import com.ByteKnights.com.resturarent_system.dto.LoginResponse;
import com.ByteKnights.com.resturarent_system.dto.StaffLoginRequest;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("auth open");
    }

    @PostMapping("/ping-post")
    public ResponseEntity<String> pingPost() {
        return ResponseEntity.ok("post open");
    }

    @PostMapping("/staff/login")
    public ResponseEntity<LoginResponse> staffLogin(@RequestBody StaffLoginRequest request) {
        System.out.println(">>> staff login endpoint reached");
        LoginResponse response = authService.loginStaff(request);
        return ResponseEntity.ok(response);
    }
}
