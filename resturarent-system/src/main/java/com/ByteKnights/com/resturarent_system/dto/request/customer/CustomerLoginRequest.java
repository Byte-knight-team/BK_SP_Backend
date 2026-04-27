package com.ByteKnights.com.resturarent_system.dto.request.customer;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CustomerLoginRequest {
    private String email;
    private String password;
}
