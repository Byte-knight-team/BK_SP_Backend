package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.manager.ManagerSalesSummaryDTO;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.entity.OrderType;
import com.ByteKnights.com.resturarent_system.entity.PaymentMethod;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.repository.PaymentRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
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
    private final StaffRepository staffRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm a");

    @Override
    public ManagerSalesSummaryDTO getSalesSummary(Long targetBranchId, Long userId) {
        Long finalBranchId = resolveBranchId(targetBranchId, userId);
        
        // 1. Gross Sales (COMPLETED + REFUNDED)
        List<OrderStatus> grossStatuses = Arrays.asList(OrderStatus.COMPLETED, OrderStatus.REFUNDED);
        BigDecimal grossSales = orderRepository.sumFinalAmountByBranchIdAndStatusIn(finalBranchId, grossStatuses);
        grossSales = (grossSales != null) ? grossSales : BigDecimal.ZERO;

        // 2. Total Refunds
        BigDecimal totalRefunds = orderRepository.sumFinalAmountByBranchIdAndStatusIn(finalBranchId, List.of(OrderStatus.REFUNDED));
        totalRefunds = (totalRefunds != null) ? totalRefunds : BigDecimal.ZERO;

        // 3. Net Sales
        BigDecimal netSales = grossSales.subtract(totalRefunds);

        // 4. Payment Methods
        BigDecimal cardPayments = paymentRepository.sumAmountByBranchIdAndPaymentMethod(finalBranchId, PaymentMethod.CARD);
        cardPayments = (cardPayments != null) ? cardPayments : BigDecimal.ZERO;

        BigDecimal cashPayments = paymentRepository.sumAmountByBranchIdAndPaymentMethod(finalBranchId, PaymentMethod.CASH);
        cashPayments = (cashPayments != null) ? cashPayments : BigDecimal.ZERO;

        // 5. Source Breakdown
        BigDecimal dineIn = orderRepository.sumFinalAmountByBranchIdAndOrderTypeAndStatusIn(
                finalBranchId, OrderType.QR, List.of(OrderStatus.COMPLETED, OrderStatus.REFUNDED));
        dineIn = (dineIn != null) ? dineIn : BigDecimal.ZERO;
        
        BigDecimal delivery = orderRepository.sumFinalAmountByBranchIdAndOrderTypeAndStatusIn(
                finalBranchId, OrderType.ONLINE_DELIVERY, List.of(OrderStatus.COMPLETED, OrderStatus.REFUNDED));
        delivery = (delivery != null) ? delivery : BigDecimal.ZERO;

        // 6. Recent Transactions
        List<ManagerSalesSummaryDTO.TransactionDTO> transactions = orderRepository.findTop50ByBranchIdOrderByCreatedAtDesc(finalBranchId)
                .stream()
                .map(order -> ManagerSalesSummaryDTO.TransactionDTO.builder()
                        .id(order.getOrderNumber() != null ? order.getOrderNumber() : "ORD-" + order.getId())
                        .date(order.getCreatedAt().format(DATE_FORMATTER))
                        .customer(order.getContactName() != null ? order.getContactName() : "Walk-in")
                        .mode(order.getPaymentStatus().name()) 
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

    private Long resolveBranchId(Long targetBranchId, Long userId) {
        if (targetBranchId != null) return targetBranchId;
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not assigned to any branch as staff"));
        if (staff.getBranch() == null) {
            throw new IllegalArgumentException("Staff member is not assigned to a branch");
        }
        return staff.getBranch().getId();
    }
}
