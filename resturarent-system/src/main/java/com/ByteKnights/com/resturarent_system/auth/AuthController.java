package com.ByteKnights.com.resturarent_system.auth;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ByteKnights.com.resturarent_system.auth.dto.LoginResponse;
import com.ByteKnights.com.resturarent_system.auth.dto.StaffLoginRequest;
import com.ByteKnights.com.resturarent_system.user.User;
import com.ByteKnights.com.resturarent_system.user.UserRepository;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/staff/login")
    public ResponseEntity<?> login(@RequestBody StaffLoginRequest request) {

        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid email");
        }

        User user = userOptional.get();

        if (!user.getPassword().equals(request.getPassword())) {
            return ResponseEntity.badRequest().body("Invalid password");
        }

        if (!user.isActive()) {
            return ResponseEntity.badRequest().body("Account disabled");
        }

        LoginResponse response = new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),   // if role is enum
                user.getFullName(),
                user.isActive()
        );

        return ResponseEntity.ok(response);
    }
}
