package com.ByteKnights.com.resturarent_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    // TODO: Inject UserService

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(/* TODO: @AuthenticationPrincipal or SecurityContext */) {
        // TODO: Return current authenticated user's profile
        return null;
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(/* TODO: @RequestBody ProfileUpdateRequest request */) {
        // TODO: Update current user's profile
        return null;
    }
}
