package com.ByteKnights.com.resturarent_system.config;

import com.ByteKnights.com.resturarent_system.entity.InviteStatus;
import com.ByteKnights.com.resturarent_system.entity.Role;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.repository.RoleRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class SuperAdminBootstrapConfig {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedSuperAdmin() {
        return args -> {
            if (userRepository.findByEmail("superadmin01@trial.com").isPresent()) {
                return;
            }

            Role superAdminRole = roleRepository.findByName("SUPER_ADMIN")
                    .orElseThrow(() -> new RuntimeException("SUPER_ADMIN role not found"));

            User superAdmin = User.builder()
                    .fullName("System Super Admin")
                    .username("superadmin01")
                    .email("superadmin01@trial.com")
                    .phone("0710000001")
                    .password(passwordEncoder.encode("Pwd@123"))
                    .role(superAdminRole)
                    .isActive(true)
                    .passwordChanged(true)
                    .inviteStatus(InviteStatus.SENT)
                    .emailSent(true)
                    .build();

            userRepository.save(superAdmin);
        };
    }
}