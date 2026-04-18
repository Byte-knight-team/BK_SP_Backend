package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.CustomerLoginRequest;
import com.ByteKnights.com.resturarent_system.dto.request.CustomerRegisterRequest;
import com.ByteKnights.com.resturarent_system.dto.response.CustomerLoginResponseData;
import com.ByteKnights.com.resturarent_system.dto.response.CustomerRegisterResponseData;
import com.ByteKnights.com.resturarent_system.entity.Customer;
import com.ByteKnights.com.resturarent_system.entity.Role;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.exception.CustomerAuthException;
import com.ByteKnights.com.resturarent_system.repository.CustomerRepository;
import com.ByteKnights.com.resturarent_system.repository.RoleRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.security.CustomerJwtService;
import com.ByteKnights.com.resturarent_system.service.CustomerAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class CustomerAuthServiceImpl implements CustomerAuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerJwtService customerJwtService;

    public CustomerAuthServiceImpl(UserRepository userRepository,
                                   RoleRepository roleRepository,
                                   CustomerRepository customerRepository,
                                   PasswordEncoder passwordEncoder,
                                   CustomerJwtService customerJwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.customerJwtService = customerJwtService;
    }

    @Override
    @Transactional
    public CustomerRegisterResponseData register(CustomerRegisterRequest request) {
        validateRegisterRequest(request);

        if (userRepository.findByEmail(request.getEmail().trim()).isPresent()) {
            throw new CustomerAuthException(HttpStatus.CONFLICT, "Email already exists");
        }

        Role customerRole = findCustomerRole();

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim().toLowerCase(Locale.ROOT))
                .phone(request.getPhone().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .address(normalizeAddress(request.getAddress()))
                .role(customerRole)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        Customer customer = Customer.builder()
                .user(savedUser)
                .build();
        customerRepository.save(customer);

        String normalizedRole = normalizeRole(customerRole.getName());
        String token = customerJwtService.generateToken(savedUser.getId(), savedUser.getEmail(), normalizedRole);

        String createdAtUtc = savedUser.getCreatedAt()
                .atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);

        return CustomerRegisterResponseData.builder()
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .role(normalizedRole)
                .token(token)
                .createdAt(createdAtUtc)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerLoginResponseData login(CustomerLoginRequest request) {
        validateLoginRequest(request);

        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new CustomerAuthException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (user.getIsActive() == false) {
            throw new CustomerAuthException(HttpStatus.FORBIDDEN, "Account is deactivated");
        }

        if (!isCustomerUser(user)) {
            throw new CustomerAuthException(HttpStatus.FORBIDDEN, "Only customers can login");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomerAuthException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        String normalizedRole = normalizeRole(user.getRole().getName());
        String token = customerJwtService.generateToken(user.getId(), user.getEmail(), normalizedRole);

        return CustomerLoginResponseData.builder()
                .userId(user.getId())
                .role(normalizedRole)
                .token(token)
                .build();
    }

    private void validateRegisterRequest(CustomerRegisterRequest request) {
        if (request == null
                || isBlank(request.getUsername())
                || isBlank(request.getEmail())
                || isBlank(request.getPhone())
                || isBlank(request.getPassword())) {
            throw new CustomerAuthException(HttpStatus.BAD_REQUEST,
                    "Username, email, phone and password are required");
        }

        if (!EMAIL_PATTERN.matcher(request.getEmail().trim()).matches()) {
            throw new CustomerAuthException(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid email format");
        }

        if (!PASSWORD_PATTERN.matcher(request.getPassword()).matches()) {
            throw new CustomerAuthException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Password must meet security requirements");
        }
    }

    private void validateLoginRequest(CustomerLoginRequest request) {
        if (request == null || isBlank(request.getEmail()) || isBlank(request.getPassword())) {
            throw new CustomerAuthException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        if (!EMAIL_PATTERN.matcher(request.getEmail().trim()).matches()) {
            throw new CustomerAuthException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }

    private Role findCustomerRole() {
        Optional<Role> roleOptional = roleRepository.findByName("ROLE_CUSTOMER");
        if (roleOptional.isPresent()) {
            return roleOptional.get();
        }

        return roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new CustomerAuthException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"));
    }

    private String normalizeRole(String roleName) {
        if (roleName == null) {
            return "CUSTOMER";
        }
        return roleName.startsWith("ROLE_") ? roleName.substring("ROLE_".length()) : roleName;
    }

    private String normalizeAddress(String address) {
        return isBlank(address) ? null : address.trim();
    }

    private boolean isCustomerUser(User user) {
        if (user == null || user.getRole() == null || isBlank(user.getRole().getName())) {
            return false;
        }

        String normalizedRole = normalizeRole(user.getRole().getName());
        if (!"CUSTOMER".equalsIgnoreCase(normalizedRole)) {
            return false;
        }

        return customerRepository.findByUser(user).isPresent();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}