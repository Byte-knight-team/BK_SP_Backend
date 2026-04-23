package com.ByteKnights.com.resturarent_system.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStaffRequest {
    private String fullName;
    private String email;
    private String phone;
    private String roleName;
    private Long branchId;
}