package com.ByteKnights.com.resturarent_system.dto;

import com.ByteKnights.com.resturarent_system.entity.InviteStatus;
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
public class StaffResponse {

    private Long id;
    private Long userId;
    private String fullName;
    private String username;
    private String email;
    private String phone;
    private String roleName;
    private Boolean active;
    private Boolean passwordChanged;
    private InviteStatus inviteStatus;
    private Boolean emailSent;
    private Long branchId;
    private String branchName;
    private String employmentStatus;
    private BigDecimal salary;
}