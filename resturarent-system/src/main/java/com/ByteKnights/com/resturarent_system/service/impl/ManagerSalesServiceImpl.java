package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.manager.ManagerSalesSummaryDTO;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.entity.OrderType;
import com.ByteKnights.com.resturarent_system.entity.PaymentMethod;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.repository.PaymentRepository;
import com.ByteKnights.com.resturarent_system.service.ManagerSalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerSalesServiceImpl implements ManagerSalesService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm a");

    @Override
    public ManagerSalesSummaryDTO getSalesSummary(Long branchId) {
        // 1. Gross Sales (COMPLETED + REFUNDED)
        List<OrderStatus> grossStatuses = Arrays.asList(OrderStatus.COMPLETED, OrderStatus.REFUNDED);
        BigDecimal grossSales = orderRepository.sumFinalAmountByBranchIdAndStatusIn(branchId, grossStatuses);

        // 2. Total Refunds
        BigDecimal totalRefunds = orderRepository.sumFinalAmountByBranchIdAndStatusIn(branchId, List.of(OrderStatus.REFUNDED));

        // 3. Net Sales
        BigDecimal netSales = grossSales.subtract(totalRefunds);

        // 4. Payment Methods
        BigDecimal cardPayments = paymentRepository.sumAmountByBranchIdAndPaymentMethod(branchId, PaymentMethod.CARD);
        BigDecimal cashPayments = paymentRepository.sumAmountByBranchIdAndPaymentMethod(branchId, PaymentMethod.CASH);

        // 5. Source Breakdown
        BigDecimal dineIn = orderRepository.sumFinalAmountByBranchIdAndOrderTypeAndStatusIn(
                branchId, OrderType.QR, List.of(OrderStatus.COMPLETED, OrderStatus.REFUNDED));
        
        BigDecimal delivery = orderRepository.sumFinalAmountByBranchIdAndOrderTypeAndStatusIn(
                branchId, OrderType.ONLINE_DELIVERY, List.of(OrderStatus.COMPLETED, OrderStatus.REFUNDED));

        // 6. Recent Transactions
        List<ManagerSalesSummaryDTO.TransactionDTO> transactions = orderRepository.findTop50ByBranchIdOrderByCreatedAtDesc(branchId)
                .stream()
                .map(order -> ManagerSalesSummaryDTO.TransactionDTO.builder()
                        .id(order.getOrderNumber() != null ? order.getOrderNumber() : "ORD-" + order.getId())
                        .date(order.getCreatedAt().format(DATE_FORMATTER))
                        .customer(order.getContactName() != null ? order.getContactName() : "Walk-in")
                        .mode(order.getPaymentStatus().name()) // Mapping status for now as proxy for mode if logic is simple
                        .amount(order.getFinalAmount())
                        .status(order.getStatus().name())
                        .build())
                .collect(Collectors.toList());

        return ManagerSalesSummaryDTO.builder()
                .grossSales(grossSales)
                .netSales(netSales)
                .totalRefunds(totalRefunds)
                .cardPayments(cardPayments)
                .cashPayments(cashPayments)
                .dineInOrders(dineIn)
                .deliveryOrders(delivery)
                .transactions(transactions)
                .build();
    }
}
