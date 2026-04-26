package com.ByteKnights.com.resturarent_system.dto.request.customer;

import lombok.Data;

@Data
public class CustomerPasswordUpdateRequest {
    private String currentPassword;
    private String newPassword;
}