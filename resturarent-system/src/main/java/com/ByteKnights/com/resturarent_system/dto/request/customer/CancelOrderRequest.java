package com.ByteKnights.com.resturarent_system.dto.request.customer;

import lombok.Data;

@Data
public class CancelOrderRequest {
    private String cancellationReason;
}
