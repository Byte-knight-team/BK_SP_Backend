package com.ByteKnights.com.resturarent_system.dto.request.customer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QrSessionStartRequest {
    
    @JsonProperty("qr_token")
    private String qrToken;

}