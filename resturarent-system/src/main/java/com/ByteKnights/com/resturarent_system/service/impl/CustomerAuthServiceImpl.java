package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.customer.CustomerLoginRequest;
import com.ByteKnights.com.resturarent_system.dto.request.customer.CustomerRegisterRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CustomerLoginResponseData;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CustomerRegisterResponseData;
import com.ByteKnights.com.resturarent_system.entity.Customer;
import com.ByteKnights.com.resturarent_system.entity.Role;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.exception.CustomerAuthException;
import com.ByteKnights.com.resturarent_system.repository.CustomerRepository;
import com.ByteKnights.com.resturarent_system.repository.RoleRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.security.CustomerJwtService;
import com.ByteKnights.com.resturarent_system.service.CustomerAuthService;
import com.ByteKnights.com.resturarent_system.service.impl.SmsService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;

@Service
public class CustomerAuthServiceImpl implements CustomerAuthService {

    //initialize email and pattern regexes
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerJwtService customerJwtService;
    private final SmsService smsService;


    public CustomerAuthServiceImpl(UserRepository userRepository,
                                   RoleRepository roleRepository,
                                   CustomerRepository customerRepository,
                                   PasswordEncoder passwordEncoder,
                                   CustomerJwtService customerJwtService,
                                SmsService smsService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.customerJwtService = customerJwtService;
        this.smsService = smsService;
    }

    //register function

    @Override
    @Transactional
    public CustomerRegisterResponseData register(CustomerRegisterRequest request) {
        validateRegisterRequest(request);

        //Check if email already exsist
        if (userRepository.findByEmail(request.getEmail().trim().toLowerCase(Locale.ROOT)).isPresent()) {
            throw new CustomerAuthException(HttpStatus.CONFLICT, "Email already exists");
        }

        //Check if Username exists
        if (userRepository.findByUsername(request.getUsername().trim()).isPresent()) {
            throw new CustomerAuthException(HttpStatus.CONFLICT, "Username already exists");
        }

        //Check if Phone exists
        if (userRepository.findByPhone(request.getPhone().trim()).isPresent()) {
            throw new CustomerAuthException(HttpStatus.CONFLICT, "Phone number already exists");
        }

        //getting role entity for customer from database
        Role customerRole = findCustomerRole();

        //build user entity
        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim().toLowerCase(Locale.ROOT))
                .phone(request.getPhone().trim())
                .passwordChanged(true)
                .password(passwordEncoder.encode(request.getPassword()))
                .address(normalizeAddress(request.getAddress()))
                .role(customerRole)
                .isActive(true)
                .build();

        //save it to database
        User savedUser = userRepository.save(user);

        //build customer entity
        Customer customer = Customer.builder()
                .user(savedUser)
                .build();
        customerRepository.save(customer);


        //normalize role name and create jwt token
        String normalizedRole = normalizeRole(customerRole.getName());
        String token = customerJwtService.generateToken(savedUser.getId(), savedUser.getEmail(), normalizedRole);

        //getting time as it uneffected by zone
        String createdAtUtc = savedUser.getCreatedAt()
                .atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);

        //returning response
        return CustomerRegisterResponseData.builder()
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .role(normalizedRole)
                .token(token)
                .createdAt(createdAtUtc)
                .build();
    }

    //login method

    @Override
    @Transactional(readOnly = true)
    public CustomerLoginResponseData login(CustomerLoginRequest request) {
        validateLoginRequest(request);

        //validating user and password
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

        //normalize role and create jwt token
        String normalizedRole = normalizeRole(user.getRole().getName());
        String token = customerJwtService.generateToken(user.getId(), user.getEmail(), normalizedRole);

        //return response
        return CustomerLoginResponseData.builder()
                .userId(user.getId())
                .role(normalizedRole)
                .token(token)
                .build();
    }

    @Override
    @Transactional
    public void requestOtp(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new CustomerAuthException(HttpStatus.BAD_REQUEST, "Phone number is required");
        }

        // 1. Generate 4-digit OTP
        String otpCode = String.format("%04d", new Random().nextInt(10000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5); // Valid for 5 mins

        // 2. Find user or create a "Guest" skeleton user
        User user = userRepository.findByPhone(phone.trim()).orElse(null);
        Customer customer;

        if (user == null) {
            // First time this phone has scanned a QR code! Create a guest profile.
            Role customerRole = findCustomerRole();
            user = User.builder()
                    .username("Guest_" + System.currentTimeMillis())
                    .phone(phone.trim())
                    .password(passwordEncoder.encode(otpCode)) // Dummy password
                    .role(customerRole)
                    .isActive(true)
                    .build();
            user = userRepository.save(user);

            customer = Customer.builder().user(user).phoneVerified(false).build();
        } else {
            customer = customerRepository.findByUser(user)
                    .orElseThrow(() -> new CustomerAuthException(HttpStatus.INTERNAL_SERVER_ERROR, "Customer profile missing"));
        }

        // 3. Save OTP to customer profile
        customer.setOtpCode(otpCode);
        customer.setOtpExpiry(expiry);
        customerRepository.save(customer);

        // 4. Send SMS
        smsService.sendOtpSms(phone, otpCode);
    }

    @Override
    @Transactional
    public CustomerLoginResponseData verifyOtp(String phone, String code) {
        if (phone == null || code == null) {
            throw new CustomerAuthException(HttpStatus.BAD_REQUEST, "Phone and code are required");
        }

        User user = userRepository.findByPhone(phone.trim())
                .orElseThrow(() -> new CustomerAuthException(HttpStatus.NOT_FOUND, "User not found"));

        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new CustomerAuthException(HttpStatus.INTERNAL_SERVER_ERROR, "Customer profile missing"));

        // 1. Check validity
        if (customer.getOtpCode() == null || !customer.getOtpCode().equals(code.trim())) {
            throw new CustomerAuthException(HttpStatus.UNAUTHORIZED, "Invalid OTP code");
        }
        if (customer.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new CustomerAuthException(HttpStatus.UNAUTHORIZED, "OTP code has expired");
        }

        // 2. Mark as verified and clear OTP
        customer.setPhoneVerified(true);
        customer.setOtpCode(null);
        customer.setOtpExpiry(null);
        customerRepository.save(customer);

        // 3. Issue JWT Token so they can place the order!
        String normalizedRole = normalizeRole(user.getRole().getName());
        String token = customerJwtService.generateToken(user.getId(), user.getPhone(), normalizedRole); // Use phone as subject if email is missing

        return CustomerLoginResponseData.builder()
                .userId(user.getId())
                .role(normalizedRole)
                .token(token)
                .build();
    }

    //helper method for validation

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


    //method to get role from database
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

    //method to if login user is a customer
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