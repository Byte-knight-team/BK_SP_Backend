package com.ByteKnights.com.resturarent_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // TODO: Inject AuthService / UserService / PasswordEncoder / JwtUtil

    @PostMapping("/register")
    public ResponseEntity<?> register(/* TODO: @RequestBody RegisterRequest request */) {
        // TODO: Implement user registration logic
        return ResponseEntity.ok("Register endpoint — not yet implemented");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(/* TODO: @RequestBody LoginRequest request */) {
        // TODO: Implement authentication + JWT token generation
        return ResponseEntity.ok("Login endpoint — not yet implemented");
    }
}
