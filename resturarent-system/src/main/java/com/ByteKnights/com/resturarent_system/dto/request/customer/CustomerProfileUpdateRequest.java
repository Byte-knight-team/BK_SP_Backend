package com.ByteKnights.com.resturarent_system.dto.request.customer;

import lombok.Data;

@Data
public class CustomerProfileUpdateRequest {
    private String username;
    private String phone;
    private String address;
}