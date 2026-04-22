package com.ByteKnights.com.resturarent_system.dto;

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
public class LoginResponse {

    private Long id;
    private String username;
    private String email;
    private String roleName;
    private Boolean active;
    private Boolean passwordChanged;
    private Long branchId;
    private String branchName;
    private String token;
    private String tokenType;
}