package com.ByteKnights.com.resturarent_system.dto.response.superadmin;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class SuperAdminCustomerResponse {

    private Long customerId;
    private Long userId;

    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String address;

    private Boolean active;

    private Integer loyaltyPoints;
    private BigDecimal totalSpent;

    private Boolean emailVerified;
    private Boolean phoneVerified;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}