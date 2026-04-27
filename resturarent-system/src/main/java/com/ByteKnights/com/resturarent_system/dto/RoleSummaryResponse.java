package com.ByteKnights.com.resturarent_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleSummaryResponse {

    private Long id;
    private String name;
    private String description;
    private Integer permissionCount;
    private Long activeUserCount;

    /*
        Default salary assigned to this role.

        Example:
        RECEPTIONIST -> 60000
        CHEF -> 75000
    */
    private BigDecimal baseSalary;
}