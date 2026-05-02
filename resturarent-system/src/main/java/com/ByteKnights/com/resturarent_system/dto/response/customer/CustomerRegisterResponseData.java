package com.ByteKnights.com.resturarent_system.dto.response.customer;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CustomerRegisterResponseData {
    private String token;
}
