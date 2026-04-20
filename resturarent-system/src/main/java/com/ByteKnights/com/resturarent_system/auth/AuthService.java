package com.ByteKnights.com.resturarent_system.auth;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ByteKnights.com.resturarent_system.dto.ChangePasswordRequest;
import com.ByteKnights.com.resturarent_system.dto.LoginResponse;
import com.ByteKnights.com.resturarent_system.dto.StaffLoginRequest;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.security.JwtService;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;

import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       StaffRepository staffRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse loginStaff(StaffLoginRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        if (userOptional.isEmpty()) throw new RuntimeException("Invalid email");

        User user = userOptional.get();
        if (!Boolean.TRUE.equals(user.getIsActive())) throw new RuntimeException("Account disabled");
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new RuntimeException("Invalid password");

        Staff staff = staffRepository.findByUserId(user.getId()).orElse(null);

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().getName());

        return LoginResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roleName(user.getRole().getName())
                .active(user.getIsActive())
                .passwordChanged(user.getPasswordChanged())
                .branchId(staff != null && staff.getBranch() != null ? staff.getBranch().getId() : null)
                .branchName(staff != null && staff.getBranch() != null ? staff.getBranch().getName() : null)
                .token(token)
                .tokenType("Bearer")
                .build();
    }

    @Transactional
    public String changePassword(ChangePasswordRequest request) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email;

        if (principal instanceof JwtUserPrincipal jwtUser) {
            email = jwtUser.getEmail();
        } else if (principal instanceof User user) {
            email = user.getEmail();
        } else {
            throw new RuntimeException("Authenticated user not found");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!Boolean.TRUE.equals(user.getIsActive())) throw new RuntimeException("Account disabled");
        if (request.getCurrentPassword() == null || request.getCurrentPassword().trim().isEmpty())
            throw new RuntimeException("Current password is required");
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty())
            throw new RuntimeException("New password is required");
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword()))
            throw new RuntimeException("Current password is incorrect");

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChanged(true);
        userRepository.save(user);

        return "Password changed successfully";
    }
}