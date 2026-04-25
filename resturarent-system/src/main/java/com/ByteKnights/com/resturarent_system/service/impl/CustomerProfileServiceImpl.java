package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.customer.CustomerPasswordUpdateRequest;
import com.ByteKnights.com.resturarent_system.dto.request.customer.CustomerProfileUpdateRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CustomerProfileResponse;
import com.ByteKnights.com.resturarent_system.entity.Customer;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.exception.CustomerAuthException;
import com.ByteKnights.com.resturarent_system.repository.CustomerRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.service.CustomerProfileService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerProfileServiceImpl(UserRepository userRepository, CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerProfileResponse getCustomerProfile(String identifier) {
        //Fetch the User
        User user = userRepository.findByEmail(identifier)
                .orElseGet(() -> userRepository.findByPhone(identifier)
                        .orElseThrow(() -> new CustomerAuthException(HttpStatus.NOT_FOUND, "User not found")));

        //Fetch the connected Customer details
        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new CustomerAuthException(HttpStatus.NOT_FOUND, "Customer profile not found"));

        //Format the date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
        String formattedDate = user.getCreatedAt().format(formatter);

        //Build and return the response
        return CustomerProfileResponse.builder()// Mapping DB username to frontend fullName
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .loyaltyPoints(customer.getLoyaltyPoints())
                .memberSince(formattedDate)
                .build();
    }

    @Override
    @Transactional
    public CustomerProfileResponse updateCustomerProfile(String currentIdentifier, CustomerProfileUpdateRequest request) {
        //Find the current user
        User user = userRepository.findByEmail(currentIdentifier)
                .orElseGet(() -> userRepository.findByPhone(currentIdentifier)
                        .orElseThrow(() -> new CustomerAuthException(HttpStatus.NOT_FOUND, "User not found")));

        //Check and Update Username
        String newUsername = request.getUsername().trim();
        if (!user.getUsername().equalsIgnoreCase(newUsername)) {
            if (userRepository.findByUsername(newUsername).isPresent()) {
                throw new CustomerAuthException(HttpStatus.CONFLICT, "Username is already taken");
            }
            user.setUsername(newUsername);
        }

        //Check and Update Phone
        String newPhone = request.getPhone().trim();
        if (!user.getPhone().equals(newPhone)) {
            if (userRepository.findByPhone(newPhone).isPresent()) {
                throw new CustomerAuthException(HttpStatus.CONFLICT, "Phone number is already in use");
            }
            user.setPhone(newPhone);
        }

        //Update Address (No unique check needed for address)
        user.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);

        //Save to DB
        userRepository.save(user);

        //Return the updated profile (reusing your existing GET logic!)
        return getCustomerProfile(user.getEmail() != null ? user.getEmail() : user.getPhone()); 
    }

    @Override
    @Transactional
    public void updatePassword(String identifier, CustomerPasswordUpdateRequest request) {
        //Find the user
        User user = userRepository.findByEmail(identifier)
                .orElseGet(() -> userRepository.findByPhone(identifier)
                        .orElseThrow(() -> new CustomerAuthException(HttpStatus.NOT_FOUND, "User not found")));

        //Verify the current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new CustomerAuthException(HttpStatus.UNAUTHORIZED, "Incorrect current password");
        }

        // 3. ENFORCE THE REGEX RULES
        if (!PASSWORD_PATTERN.matcher(request.getNewPassword()).matches()) {
            throw new CustomerAuthException(HttpStatus.UNPROCESSABLE_ENTITY, 
                    "Password must be at least 8 characters, with 1 uppercase, 1 lowercase, 1 number, and 1 special character");
        }

        // 4. Encode and save the new password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }


}