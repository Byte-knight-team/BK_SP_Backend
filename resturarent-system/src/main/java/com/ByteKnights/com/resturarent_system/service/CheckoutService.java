package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.customer.CheckoutCalculateRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CheckoutCalculateResponse;

public interface CheckoutService {
    CheckoutCalculateResponse calculateOrderTotals(String userIdentifier, CheckoutCalculateRequest request);
}