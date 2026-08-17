package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.receptionist.PagedResponse;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistOrderDetailDTO;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistOrderHistoryDTO;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistOrderSummaryDTO;
import java.util.List;

public interface ReceptionistOrderService {
    List<ReceptionistOrderSummaryDTO> getOrdersByStatus(String status, String userEmail);
    ReceptionistOrderDetailDTO getOrderDetail(Long orderId, String userEmail);
    void sendToKitchen(Long orderId, String userEmail);
    void holdOrder(Long orderId, String reason, String userEmail);
    void cancelOrder(Long orderId, String reason, String userEmail);
    void collectPayment(Long orderId, java.math.BigDecimal cashReceived, String userEmail);
    void serveOrder(Long orderId, String userEmail);
    void serveOrderItem(Long itemId, String userEmail);
    PagedResponse<ReceptionistOrderHistoryDTO> getOrderHistory(
            String userEmail, int page, int size, String date, String status, String orderType, String paymentStatus);
}
