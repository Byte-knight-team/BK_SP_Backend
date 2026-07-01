package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.superadmin.SuperAdminCustomerResponse;
import com.ByteKnights.com.resturarent_system.entity.AuditEventType;
import com.ByteKnights.com.resturarent_system.entity.AuditModule;
import com.ByteKnights.com.resturarent_system.entity.AuditSeverity;
import com.ByteKnights.com.resturarent_system.entity.AuditStatus;
import com.ByteKnights.com.resturarent_system.entity.AuditTargetType;
import com.ByteKnights.com.resturarent_system.entity.Customer;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.exception.CustomerAuthException;
import com.ByteKnights.com.resturarent_system.repository.CustomerRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.SuperAdminCustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SuperAdminCustomerServiceImpl implements SuperAdminCustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public SuperAdminCustomerServiceImpl(CustomerRepository customerRepository,
                                         UserRepository userRepository,
                                         AuditLogService auditLogService) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    /*
     * SUPER_ADMIN: Get all customers.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SuperAdminCustomerResponse> getAllCustomers() {
        return customerRepository.findAllWithUserOrderByIdDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /*
     * SUPER_ADMIN: Get one customer by customer ID.
     */
    @Override
    @Transactional(readOnly = true)
    public SuperAdminCustomerResponse getCustomerById(Long customerId) {
        Customer customer = getCustomerOrThrow(customerId);
        return mapToResponse(customer);
    }

    /*
     * SUPER_ADMIN: Activate customer account.
     * The active status is stored in the connected User entity.
     */
    @Override
    @Transactional
    public SuperAdminCustomerResponse activateCustomer(Long customerId) {
        Customer customer = getCustomerOrThrow(customerId);
        User user = customer.getUser();

        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new CustomerAuthException(HttpStatus.BAD_REQUEST, "Customer account is already active");
        }

        Map<String, Object> oldValues = buildStatusAuditValues(user);

        user.setIsActive(true);
        User savedUser = userRepository.save(user);

        Map<String, Object> newValues = buildStatusAuditValues(savedUser);

        auditLogService.logCurrentUserAction(
                AuditModule.CUSTOMER,
                AuditEventType.CUSTOMER_ACTIVATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.CUSTOMER,
                customer.getId(),
                null,
                "Customer account activated by SUPER_ADMIN",
                oldValues,
                newValues
        );

        return mapToResponse(customer);
    }

    /*
     * SUPER_ADMIN: Deactivate customer account.
     * Once deactivated, customer login and protected customer APIs must be blocked.
     */
    @Override
    @Transactional
    public SuperAdminCustomerResponse deactivateCustomer(Long customerId) {
        Customer customer = getCustomerOrThrow(customerId);
        User user = customer.getUser();

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new CustomerAuthException(HttpStatus.BAD_REQUEST, "Customer account is already inactive");
        }

        Map<String, Object> oldValues = buildStatusAuditValues(user);

        user.setIsActive(false);
        User savedUser = userRepository.save(user);

        Map<String, Object> newValues = buildStatusAuditValues(savedUser);

        auditLogService.logCurrentUserAction(
                AuditModule.CUSTOMER,
                AuditEventType.CUSTOMER_DEACTIVATED,
                AuditStatus.SUCCESS,
                AuditSeverity.WARN,
                AuditTargetType.CUSTOMER,
                customer.getId(),
                null,
                "Customer account deactivated by SUPER_ADMIN",
                oldValues,
                newValues
        );

        return mapToResponse(customer);
    }

    /*
     * Load customer with connected User.
     */
    private Customer getCustomerOrThrow(Long customerId) {
        return customerRepository.findByIdWithUser(customerId)
                .orElseThrow(() -> new CustomerAuthException(HttpStatus.NOT_FOUND, "Customer not found"));
    }

    /*
     * Convert Customer + User into response DTO.
     */
    private SuperAdminCustomerResponse mapToResponse(Customer customer) {
        User user = customer.getUser();

        return SuperAdminCustomerResponse.builder()
                .customerId(customer.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .active(user.getIsActive())
                .loyaltyPoints(customer.getLoyaltyPoints())
                .totalSpent(customer.getTotalSpent())
                .emailVerified(customer.getEmailVerified())
                .phoneVerified(customer.getPhoneVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /*
     * Audit values for activate/deactivate.
     * Do not include password or sensitive token data.
     */
    private Map<String, Object> buildStatusAuditValues(User user) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("userId", user.getId());
        values.put("email", user.getEmail());
        values.put("phone", user.getPhone());
        values.put("username", user.getUsername());
        values.put("active", user.getIsActive());
        return values;
    }
}