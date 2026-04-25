package com.ByteKnights.com.resturarent_system.dto.request.customer;

import lombok.Data;

@Data
public class PaymentUpdateRequest {
    private String paymentStatus;
    private String transactionId;
}