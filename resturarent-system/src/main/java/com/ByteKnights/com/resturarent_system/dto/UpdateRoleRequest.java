package com.ByteKnights.com.resturarent_system.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateRoleRequest {

    private String name;
    private String description;

    /*
        Default salary for this role.

        This does not automatically update existing staff salaries.
        It affects new staff creation unless salary is manually overridden.
    */
    private BigDecimal baseSalary;
}