package com.ByteKnights.com.resturarent_system.dto.response.customer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QrSessionStartResponseData {
    @JsonProperty("session_token")
    private String sessionToken;
}