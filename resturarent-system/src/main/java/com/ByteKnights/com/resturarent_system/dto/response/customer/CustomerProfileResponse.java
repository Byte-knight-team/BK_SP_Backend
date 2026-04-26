package com.ByteKnights.com.resturarent_system.dto.response.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileResponse {
    private String username;
    private String email;
    private String phone;
    private String address;
    private Integer loyaltyPoints;
    private String memberSince;
}