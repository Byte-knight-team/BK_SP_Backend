package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.customer.CustomerLoginRequest;
import com.ByteKnights.com.resturarent_system.dto.request.customer.CustomerRegisterRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CustomerLoginResponseData;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CustomerRegisterResponseData;
import com.ByteKnights.com.resturarent_system.entity.Customer;
import com.ByteKnights.com.resturarent_system.entity.PasswordResetToken;
import com.ByteKnights.com.resturarent_system.entity.QrSession;
import com.ByteKnights.com.resturarent_system.entity.Role;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.exception.CustomerAuthException;
import com.ByteKnights.com.resturarent_system.repository.CustomerRepository;
import com.ByteKnights.com.resturarent_system.repository.PasswordResetTokenRepository;
import com.ByteKnights.com.resturarent_system.repository.QrSessionRepository;
import com.ByteKnights.com.resturarent_system.repository.RoleRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.security.CustomerJwtService;
import com.ByteKnights.com.resturarent_system.service.CustomerAuthService;
import com.ByteKnights.com.resturarent_system.service.ProfileImageStorageService;
import com.ByteKnights.com.resturarent_system.service.email.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;

@Service
public class CustomerAuthServiceImpl implements CustomerAuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PASSWORD_PATTERN = Pattern
            .compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final QrSessionRepository qrSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerJwtService customerJwtService;
    private final SmsService smsService;
    private final ProfileImageStorageService profileImageStorageService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    @Value("${app.frontend.customer-base-url:https://cravehouse.netlify.app}")
    private String customerFrontendBaseUrl;

    public CustomerAuthServiceImpl(UserRepository userRepository,
                                   RoleRepository roleRepository,
                                   CustomerRepository customerRepository,
                                   QrSessionRepository qrSessionRepository,
                                   PasswordEncoder passwordEncoder,
                                   CustomerJwtService customerJwtService,
                                   SmsService smsService,
                                   ProfileImageStorageService profileImageStorageService,
                                   PasswordResetTokenRepository passwordResetTokenRepository,
                                   EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.customerRepository = customerRepository;
        this.qrSessionRepository = qrSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.customerJwtService = customerJwtService;
        this.smsService = smsService;
        this.profileImageStorageService = profileImageStorageService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public CustomerRegisterResponseData register(CustomerRegisterRequest request) {
        validateRegisterRequest(request);

        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String normalizedPhone = request.getPhone().trim();
        String normalizedUsername = request.getUsername().trim();

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new CustomerAuthException(HttpStatus.CONFLICT, "Email already exists");
        }

        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new CustomerAuthException(HttpStatus.CONFLICT, "Username already exists");
        }

        Optional<User> existingUserOpt = userRepository.findByPhone(normalizedPhone);
        User user;
        boolean isGhostAccount = false;

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            /*
             * If this phone belongs to a deactivated customer/ghost account,
             * do not allow registration to reactivate it.
             */
            ensureCustomerAccountActive(existingUser);

            /*
             * If the user has an email, it is a real, fully registered account.
             */
            if (existingUser.getEmail() != null && !existingUser.getEmail().isEmpty()) {
                throw new CustomerAuthException(HttpStatus.CONFLICT,
                        "Phone number already exists and is linked to an account.");
            } else {
                /*
                 * No email means this is a QR ghost account.
                 * Upgrade it into a registered customer account.
                 */
                user = existingUser;
                isGhostAccount = true;
            }
        } else {
            user = new User();
            user.setRole(findCustomerRole());
        }

        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPhone(normalizedPhone);
        user.setPasswordChanged(true);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAddress(normalizeAddress(request.getAddress()));
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        if (!isGhostAccount) {
            Customer customer = Customer.builder()
                    .user(savedUser)
                    .build();
            customerRepository.save(customer);
        }

        Role customerRole = findCustomerRole();

        String normalizedRole = normalizeRole(customerRole.getName());
        String token = customerJwtService.generateToken(savedUser.getId(), savedUser.getEmail(), normalizedRole);

        return CustomerRegisterResponseData.builder()
                .token(token)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerLoginResponseData login(CustomerLoginRequest request) {
        validateLoginRequest(request);

        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new CustomerAuthException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        ensureCustomerAccountActive(user);

        if (!isCustomerUser(user)) {
            throw new CustomerAuthException(HttpStatus.FORBIDDEN, "Only customers can login");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomerAuthException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        String normalizedRole = normalizeRole(user.getRole().getName());
        String token = customerJwtService.generateToken(user.getId(), user.getEmail(), normalizedRole);

        String profilePictureUrl = null;
        if (user.getProfilePictureKey() != null) {
            profilePictureUrl = profileImageStorageService.createPresignedDownloadUrl(user.getProfilePictureKey());
        }

        return CustomerLoginResponseData.builder()
                .token(token)
                .username(user.getUsername())
                .profilePictureUrl(profilePictureUrl)
                .build();
    }

    @Override
    @Transactional
    public void requestOtp(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new CustomerAuthException(HttpStatus.BAD_REQUEST, "Phone number is required");
        }

        String otpCode = String.format("%04d", new Random().nextInt(10000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        User user = userRepository.findByPhone(phone.trim()).orElse(null);
        Customer customer;

        if (user == null) {
            Role customerRole = findCustomerRole();

            user = User.builder()
                    .username("Guest_" + System.currentTimeMillis())
                    .phone(phone.trim())
                    .password(passwordEncoder.encode(otpCode))
                    .role(customerRole)
                    .isActive(true)
                    .build();

            user = userRepository.save(user);

            customer = Customer.builder()
                    .user(user)
                    .phoneVerified(false)
                    .build();
        } else {
            /*
             * Existing deactivated customers must not receive OTP.
             */
            ensureCustomerAccountActive(user);

            customer = customerRepository.findByUser(user)
                    .orElseThrow(() -> new CustomerAuthException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Customer profile missing"));
        }

        customer.setOtpCode(otpCode);
        customer.setOtpExpiry(expiry);
        customerRepository.save(customer);

        smsService.sendOtpSms(phone, otpCode);
    }

    @Override
    @Transactional
    public CustomerLoginResponseData verifyOtp(String phone, String code, Long sessionId) {
        if (phone == null || code == null) {
            throw new CustomerAuthException(HttpStatus.BAD_REQUEST, "Phone and code are required");
        }

        User user = userRepository.findByPhone(phone.trim())
                .orElseThrow(() -> new CustomerAuthException(HttpStatus.NOT_FOUND, "User not found"));

        /*
         * Deactivated users must not verify OTP.
         */
        ensureCustomerAccountActive(user);

        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new CustomerAuthException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Customer profile missing"));

        if (customer.getOtpCode() == null || !customer.getOtpCode().equals(code.trim())) {
            throw new CustomerAuthException(HttpStatus.UNAUTHORIZED, "Invalid OTP code");
        }

        if (customer.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new CustomerAuthException(HttpStatus.UNAUTHORIZED, "OTP code has expired");
        }

        customer.setPhoneVerified(true);
        customer.setOtpCode(null);
        customer.setOtpExpiry(null);
        customerRepository.save(customer);

        if (sessionId != null) {
            QrSession qrSession = qrSessionRepository.findById(sessionId).orElse(null);

            if (qrSession != null && qrSession.getCustomer() == null) {
                qrSession.setCustomer(customer);
                qrSessionRepository.save(qrSession);
            }
        }

        String normalizedRole = normalizeRole(user.getRole().getName());
        String tokenSubject = user.getEmail() != null ? user.getEmail() : user.getPhone();
        String token = customerJwtService.generateToken(user.getId(), tokenSubject, normalizedRole);

        String profilePictureUrl = null;
        if (user.getProfilePictureKey() != null) {
            profilePictureUrl = profileImageStorageService.createPresignedDownloadUrl(user.getProfilePictureKey());
        }

        return CustomerLoginResponseData.builder()
                .token(token)
                .username(user.getUsername())
                .profilePictureUrl(profilePictureUrl)
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

    /*
     * Deactivated customers must not continue customer auth flows.
     */
    private void ensureCustomerAccountActive(User user) {
        if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
            throw new CustomerAuthException(HttpStatus.FORBIDDEN, "Account is deactivated");
        }
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

    @Override
    @Transactional
    public void forgotPassword(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new CustomerAuthException(HttpStatus.BAD_REQUEST, "Invalid email format");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new CustomerAuthException(HttpStatus.NOT_FOUND,
                        "No customer account found with this email"));

        if (!isCustomerUser(user)) {
            throw new CustomerAuthException(HttpStatus.FORBIDDEN, "Invalid account type for this operation");
        }

        /*
         * Deactivated customers must not start forgot-password flow.
         */
        ensureCustomerAccountActive(user);

        passwordResetTokenRepository.deleteByUser(user);
        passwordResetTokenRepository.flush();

        String tokenStr = java.util.UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(tokenStr)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .build();

        passwordResetTokenRepository.save(resetToken);

        String resetLink = customerFrontendBaseUrl + "/reset-password?token=" + tokenStr;

        emailService.sendCustomerPasswordResetEmail(normalizedEmail, resetLink);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new CustomerAuthException(HttpStatus.BAD_REQUEST, "Password cannot be empty");
        }

        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new CustomerAuthException(HttpStatus.BAD_REQUEST,
                    "Password must be at least 8 characters long, contain at least one uppercase letter, one lowercase letter, one number, and one special character");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new CustomerAuthException(HttpStatus.BAD_REQUEST, "Invalid password reset token"));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new CustomerAuthException(HttpStatus.BAD_REQUEST, "Password reset token has expired");
        }

        User user = resetToken.getUser();

        /*
         * If the account was deactivated after the reset link was created,
         * do not allow password reset to continue.
         */
        ensureCustomerAccountActive(user);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
    }
}