package com.ByteKnights.com.resturarent_system.service.impl;

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
}