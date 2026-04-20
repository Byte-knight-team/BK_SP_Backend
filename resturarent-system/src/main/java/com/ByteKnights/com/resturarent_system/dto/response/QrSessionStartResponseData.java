package com.ByteKnights.com.resturarent_system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QrSessionStartResponseData {
    
    @JsonProperty("session_id")
    private Long sessionId;

    @JsonProperty("session_token")
    private String sessionToken;

    @JsonProperty("branch_id")
    private Long branchId;

    @JsonProperty("table_id")
    private Long tableId;

    @JsonProperty("expires_at")
    private String expiresAt;

}