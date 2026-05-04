package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.delivery.DeliveryOrderDTO;
import com.ByteKnights.com.resturarent_system.entity.Delivery;
import com.ByteKnights.com.resturarent_system.entity.DeliveryStatus;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.repository.DeliveryRepository;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.service.DeliveryOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;

@Service
@RequiredArgsConstructor
public class DeliveryOrderServiceImpl implements DeliveryOrderService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final StaffRepository staffRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryOrderDTO> getAssignedOrders(Long userId) {
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Staff member not found for user ID: " + userId));

        List<Delivery> assignments = deliveryRepository.findByDeliveryStaffIdAndDeliveryStatus(staff.getId(), DeliveryStatus.ASSIGNED);

        return assignments.stream().map(d -> mapToDTO(d)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeliveryOrderDTO> getActiveOrder(Long userId) {
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Staff member not found for user ID: " + userId));

        List<Delivery> activeDeliveries = deliveryRepository.findByDeliveryStaffIdAndDeliveryStatusIn(
                staff.getId(), 
                Arrays.asList(DeliveryStatus.ACCEPTED, DeliveryStatus.OUT_FOR_DELIVERY)
        );

        if (activeDeliveries.isEmpty()) {
            return Optional.empty();
        }

        // Return the first one (assuming one active delivery at a time)
        return Optional.of(mapToDTO(activeDeliveries.get(0)));
    }

    private DeliveryOrderDTO mapToDTO(Delivery d) {
        return DeliveryOrderDTO.builder()
                .id(d.getOrder().getId())
                .orderNumber(d.getOrder().getOrderNumber() != null ? d.getOrder().getOrderNumber() : "ORD-" + d.getOrder().getId())
                .location(d.getOrder().getDeliveryAddress())
                .paymentType("CASH ON DELIVERY")
                .amount(d.getOrder().getFinalAmount())
                .status(d.getDeliveryStatus().name())
                .build();
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

    @Override
    @Transactional
    public void updateStatus(Long orderId, Long userId, DeliveryStatus status) {
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Staff member not found for user ID: " + userId));

        Delivery delivery = deliveryRepository.findByOrderIdAndDeliveryStaffId(orderId, staff.getId())
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found for order ID: " + orderId));

        delivery.setDeliveryStatus(status);
        if (status == DeliveryStatus.DELIVERED) {
            delivery.setDeliveredAt(LocalDateTime.now());
            // Explicitly save the Order status to SERVED via OrderRepository,
            // because the Delivery -> Order relationship has no cascade.
            delivery.getOrder().setStatus(OrderStatus.SERVED);
            orderRepository.save(delivery.getOrder());
        }
        
        deliveryRepository.save(delivery);
    }
}
