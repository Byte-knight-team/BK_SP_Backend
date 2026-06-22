package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.customer.PaymentUpdateRequest;
import com.ByteKnights.com.resturarent_system.dto.request.customer.PlaceOrderRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CustomerOrdersPageResponse;
import com.ByteKnights.com.resturarent_system.dto.response.customer.OrderPlacementResponse;
import com.ByteKnights.com.resturarent_system.dto.response.customer.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderPlacementResponse placeCustomerOrder(String userIdentifier, PlaceOrderRequest request);
    void updatePaymentStatus(Long orderId, PaymentUpdateRequest request);
    List<OrderResponse> getCustomerOrders(String userIdentifier);
    List<OrderResponse> getCustomerOrders(String userIdentifier, String orderTypeFilter, Boolean isActive);
    CustomerOrdersPageResponse getCustomerOrdersPage(String userIdentifier, String orderTypeFilter, Boolean isActive, int page, int size);
    OrderResponse getCustomerOrderById(String userIdentifier, Long orderId);
    void cancelCustomerOrder(String userIdentifier, Long orderId, String cancelReason);
}
