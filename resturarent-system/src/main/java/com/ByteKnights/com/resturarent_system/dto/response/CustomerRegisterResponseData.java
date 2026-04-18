package com.ByteKnights.com.resturarent_system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerRegisterResponseData {
    @JsonProperty("user_id")
    private Long userId;

    private String username;
    private String role;
    private String token;

    @JsonProperty("created_at")
    private String createdAt;
}
