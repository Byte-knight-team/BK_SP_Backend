package com.ByteKnights.com.resturarent_system.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateStaffRequest {

    private String fullName;
    private String username;
    private String email;
    private String phone;
    private String roleName;
    private Long branchId;

    /*
        Optional salary override.

        If this value is provided:
        - Staff.salary uses this value.

        If this value is not provided:
        - Staff.salary uses Role.baseSalary.

        This keeps staff creation simple but still allows salary adjustment.
    */
    private BigDecimal salary;
}