package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.delivery.DeliveryOrderDTO;
import com.ByteKnights.com.resturarent_system.entity.Delivery;
import com.ByteKnights.com.resturarent_system.entity.DeliveryStatus;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.repository.DeliveryRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.service.DeliveryOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryOrderServiceImpl implements DeliveryOrderService {

    private final DeliveryRepository deliveryRepository;
    private final StaffRepository staffRepository;

    @Override
    public List<DeliveryOrderDTO> getAssignedOrders(Long userId) {
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Staff member not found for user ID: " + userId));

        List<Delivery> assignments = deliveryRepository.findByDeliveryStaffIdAndDeliveryStatus(staff.getId(), DeliveryStatus.ASSIGNED);

        return assignments.stream().map(d -> DeliveryOrderDTO.builder()
                .id(d.getOrder().getId())
                .orderNumber(d.getOrder().getOrderNumber() != null ? d.getOrder().getOrderNumber() : "ORD-" + d.getOrder().getId())
                .location(d.getOrder().getDeliveryAddress())
                .paymentType("CASH ON DELIVERY") // TODO: Get from Order or Payment
                .amount(d.getOrder().getFinalAmount())
                .status(d.getDeliveryStatus().name())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void acceptOrder(Long orderId, Long userId) {
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Staff member not found for user ID: " + userId));

        Delivery delivery = deliveryRepository.findByOrderIdAndDeliveryStaffId(orderId, staff.getId())
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found for order ID: " + orderId));

        delivery.setDeliveryStatus(DeliveryStatus.ACCEPTED);
        deliveryRepository.save(delivery);
    }

    @Override
    @Transactional
    public void rejectOrder(Long orderId, Long userId, String reason) {
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Staff member not found for user ID: " + userId));

        Delivery delivery = deliveryRepository.findByOrderIdAndDeliveryStaffId(orderId, staff.getId())
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found for order ID: " + orderId));

        delivery.setDeliveryStatus(DeliveryStatus.CANCELLED);
        delivery.setCancelledReason(reason);
        deliveryRepository.save(delivery);
    }
}
