package com.ByteKnights.com.resturarent_system.dto.response.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerStaffMemberDTO {
    private Long userId;
    private String name;
    private String role;
    private String hireDate;
    private String contactNumber;
    private BigDecimal salary;
    private String status;
}
