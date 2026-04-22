package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.customer.CustomerProfileUpdateRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CustomerProfileResponse;
import com.ByteKnights.com.resturarent_system.entity.Customer;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.exception.CustomerAuthException;
import com.ByteKnights.com.resturarent_system.repository.CustomerRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.service.CustomerProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public CustomerProfileServiceImpl(UserRepository userRepository, CustomerRepository customerRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerProfileResponse getCustomerProfile(String email) {
        //Fetch the User
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomerAuthException(HttpStatus.NOT_FOUND, "User not found"));

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
    public CustomerProfileResponse updateCustomerProfile(String currentEmail, CustomerProfileUpdateRequest request) {
        //Find the current user
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new CustomerAuthException(HttpStatus.NOT_FOUND, "User not found"));

        //Check and Update Username
        String newUsername = request.getUsername().trim();
        if (!user.getUsername().equalsIgnoreCase(newUsername)) {
            if (userRepository.findByUsername(newUsername).isPresent()) {
                throw new CustomerAuthException(HttpStatus.CONFLICT, "Username is already taken");
            }
            user.setUsername(newUsername);
        }

        //Check and Update Email
        String newEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!user.getEmail().equalsIgnoreCase(newEmail)) {
            if (userRepository.findByEmail(newEmail).isPresent()) {
                throw new CustomerAuthException(HttpStatus.CONFLICT, "Email is already in use");
            }
            user.setEmail(newEmail);
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
        return getCustomerProfile(user.getEmail()); 
    }
}