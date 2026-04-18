package com.byteknights.com.resturarent_system.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateStaffRequest {
    private String username;
    private String email;
    private String phone;
    private String roleName;
}