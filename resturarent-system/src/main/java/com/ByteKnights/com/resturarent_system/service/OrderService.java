package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.customer.PaymentUpdateRequest;
import com.ByteKnights.com.resturarent_system.dto.request.customer.PlaceOrderRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.OrderResponse;

public interface OrderService {
    OrderResponse placeCustomerOrder(String userIdentifier, PlaceOrderRequest request);
    void updatePaymentStatus(Long orderId, PaymentUpdateRequest request);
}
