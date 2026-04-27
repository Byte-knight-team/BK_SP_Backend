package com.ByteKnights.com.resturarent_system.dto.request.customer;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CustomerRegisterRequest {
    private String username;
    private String email;
    private String phone;
    private String password;
    private String address;
}
