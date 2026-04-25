package com.ByteKnights.com.resturarent_system.dto.request;

import lombok.Data;

@Data
public class CustomerProfileUpdateRequest {
    private String username;
    private String email;
    private String phone;
    private String address;
}