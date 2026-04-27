package com.ByteKnights.com.resturarent_system.dto.response.customer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CustomerLoginResponseData {
    @JsonProperty("user_id")
    private Long userId;

    private String token;
}
