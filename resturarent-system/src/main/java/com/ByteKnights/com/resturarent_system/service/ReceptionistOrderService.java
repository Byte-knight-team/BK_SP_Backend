package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistOrderDetailDTO;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistOrderSummaryDTO;
import java.util.List;

public interface ReceptionistOrderService {
    List<ReceptionistOrderSummaryDTO> getOrdersByStatus(String status, String userEmail);
    ReceptionistOrderDetailDTO getOrderDetail(Long orderId, String userEmail);
    void sendToKitchen(Long orderId, String userEmail);
    void holdOrder(Long orderId, String reason, String userEmail);
    void cancelOrder(Long orderId, String reason, String userEmail);
    void collectPayment(Long orderId, String userEmail);
    void serveOrder(Long orderId, String userEmail);
    void serveOrderItem(Long itemId, String userEmail);
}
