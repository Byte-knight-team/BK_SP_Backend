package com.byteknights.com.resturarent_system.dto;

import com.byteknights.com.resturarent_system.entity.InviteStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStaffResponse {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String role;
    private Boolean active;
    private Boolean passwordChanged;
    private InviteStatus inviteStatus;
    private Boolean emailSent;
    private String temporaryPassword;
    private String message;
}