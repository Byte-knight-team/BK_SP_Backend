package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.customer.CheckoutCalculateRequest;
import com.ByteKnights.com.resturarent_system.dto.request.customer.PaymentUpdateRequest;
import com.ByteKnights.com.resturarent_system.dto.request.customer.PlaceOrderRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CheckoutCalculateResponse;
import com.ByteKnights.com.resturarent_system.dto.response.customer.OrderResponse;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.exception.CheckoutException;
import com.ByteKnights.com.resturarent_system.exception.ResourceNotFoundException;
import com.ByteKnights.com.resturarent_system.repository.*;
import com.ByteKnights.com.resturarent_system.service.CheckoutService;
import com.ByteKnights.com.resturarent_system.service.OrderService;
import com.ByteKnights.com.resturarent_system.service.QrSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

        private final CheckoutService checkoutService;
        private final QrSessionService qrSessionService;
        private final OrderRepository orderRepository;
        private final PaymentRepository paymentRepository;
        private final CustomerRepository customerRepository;
        private final UserRepository userRepository;
        private final BranchRepository branchRepository;
        private final RestaurantTableRepository tableRepository;
        private final MenuItemRepository menuItemRepository;
        private final CouponRepository couponRepository;
        private final CouponUsageRepository couponUsageRepository;
        private final LoyaltyTransactionRepository loyaltyTransactionRepository;

        public OrderServiceImpl(CheckoutService checkoutService, QrSessionService qrSessionService,
                        OrderRepository orderRepository,
                        PaymentRepository paymentRepository, CustomerRepository customerRepository,
                        UserRepository userRepository, BranchRepository branchRepository,
                        RestaurantTableRepository tableRepository, MenuItemRepository menuItemRepository,
                        CouponRepository couponRepository, CouponUsageRepository couponUsageRepository,
                        LoyaltyTransactionRepository loyaltyTransactionRepository) {
                this.checkoutService = checkoutService;
                this.qrSessionService = qrSessionService;
                this.orderRepository = orderRepository;
                this.paymentRepository = paymentRepository;
                this.customerRepository = customerRepository;
                this.userRepository = userRepository;
                this.branchRepository = branchRepository;
                this.tableRepository = tableRepository;
                this.menuItemRepository = menuItemRepository;
                this.couponRepository = couponRepository;
                this.couponUsageRepository = couponUsageRepository;
                this.loyaltyTransactionRepository = loyaltyTransactionRepository;
        }

        @Override
        @Transactional
        public OrderResponse placeCustomerOrder(String userIdentifier, PlaceOrderRequest request) {

                // 1. Resolve Customer
                User user = userRepository.findByEmail(userIdentifier)
                                .orElseGet(() -> userRepository.findByPhone(userIdentifier)
                                                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
                Customer customer = customerRepository.findByUser(user)
                                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

                // 2. Resolve Branch & Table
                Branch branch = branchRepository.findById(request.getBranchId())
                                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));

                RestaurantTable table = null;
                if (OrderType.QR.name().equals(request.getOrderType())) {
                        if (request.getTableId() == null)
                                throw new CheckoutException(HttpStatus.BAD_REQUEST, "Table ID required for Dine-In");
                        table = tableRepository.findById(request.getTableId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Table not found"));

                        // QR Session Guard: Validate the session is still ACTIVE in the database
                        if (request.getQrSessionId() == null) {
                                throw new CheckoutException(HttpStatus.BAD_REQUEST,
                                                "QR session ID is required for table orders.");
                        }
                        qrSessionService.validateActiveSession(request.getQrSessionId());
                }

                // 3. ZERO-TRUST MATH: Call the CheckoutService to calculate the exact totals!
                CheckoutCalculateRequest calcRequest = new CheckoutCalculateRequest();
                calcRequest.setOrderType(request.getOrderType());
                calcRequest.setBranchId(request.getBranchId());
                calcRequest.setCouponCode(request.getCouponCode());
                calcRequest.setRedeemLoyaltyPoints(request.getRedeemLoyaltyPoints());
                calcRequest.setItems(request.getItems());

                CheckoutCalculateResponse trustedMath = checkoutService.calculateOrderTotals(userIdentifier,
                                calcRequest);

                // 4. Build the Order Entity
                Order order = new Order();
                order.setCustomer(customer);
                order.setBranch(branch);
                order.setTable(table);
                order.setOrderType(OrderType.valueOf(request.getOrderType().toUpperCase()));
                order.setStatus(OrderStatus.PLACED);

                // Contact Details
                order.setContactName(request.getContactName());
                order.setContactPhone(request.getContactPhone());
                order.setContactEmail(request.getContactEmail());
                order.setDeliveryAddress(request.getDeliveryAddress());
                order.setKitchenNotes(request.getKitchenNotes());

                // Apply Trusted Math to Order
                order.setTaxAmount(trustedMath.getTaxAmount());
                order.setDeliveryFee(trustedMath.getDeliveryFee());
                order.setServiceCharge(trustedMath.getServiceCharge());
                order.setDiscountAmount(trustedMath.getTotalDiscountAmount());
                order.setRewardPointsEarned(trustedMath.getLoyaltyPointsEarnedThisOrder());
                order.setRewardPointsRedeemed(trustedMath.getLoyaltyPointsRedeemed());

                if (trustedMath.getAppliedCouponCode() != null) {
                        order.setAppliedCouponCode(trustedMath.getAppliedCouponCode());
                }

                // 5. Add Items to Order
                for (CheckoutCalculateRequest.CartItemRequest itemReq : request.getItems()) {
                        MenuItem dbItem = menuItemRepository.findById(itemReq.getMenuItemId()).orElseThrow();
                        OrderItem orderItem = new OrderItem();
                        orderItem.setMenuItem(dbItem);
                        orderItem.setItemName(dbItem.getName());
                        orderItem.setQuantity(itemReq.getQuantity());
                        orderItem.setUnitPrice(dbItem.getPrice());
                        orderItem.setSubtotal(dbItem.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
                        order.addItem(orderItem);
                }

                // Generate Order Number
                String uniqueSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                order.setOrderNumber("#ORD-" + uniqueSuffix);

                // 6. SAVE ORDER
                Order savedOrder = orderRepository.save(order);

                // 7. Process Coupon Usage
                if (trustedMath.getAppliedCouponCode() != null) {
                        Coupon coupon = couponRepository.findByCode(trustedMath.getAppliedCouponCode()).orElseThrow();
                        coupon.setUsedCount(coupon.getUsedCount() + 1);
                        couponRepository.save(coupon);

                        CouponUsage usage = CouponUsage.builder()
                                        .coupon(coupon).customer(customer).order(savedOrder).build();
                        couponUsageRepository.save(usage);
                }

                // 8. Process Loyalty Transactions
                // Deduct points used
                if (trustedMath.getLoyaltyPointsRedeemed() > 0) {
                        customer.setLoyaltyPoints(customer.getLoyaltyPoints() - trustedMath.getLoyaltyPointsRedeemed());
                        LoyaltyTransaction redeemTx = LoyaltyTransaction.builder()
                                        .customer(customer).order(savedOrder)
                                        .transactionType(LoyaltyTransactionType.REDEEM)
                                        .points(trustedMath.getLoyaltyPointsRedeemed())
                                        .description("Redeemed on order " + savedOrder.getOrderNumber())
                                        .build();
                        loyaltyTransactionRepository.save(redeemTx);
                }

                // Add points earned
                if (trustedMath.getLoyaltyPointsEarnedThisOrder() > 0) {
                        customer.setLoyaltyPoints(
                                        customer.getLoyaltyPoints() + trustedMath.getLoyaltyPointsEarnedThisOrder());
                        LoyaltyTransaction earnTx = LoyaltyTransaction.builder()
                                        .customer(customer).order(savedOrder)
                                        .transactionType(LoyaltyTransactionType.EARN)
                                        .points(trustedMath.getLoyaltyPointsEarnedThisOrder())
                                        .description("Earned from order " + savedOrder.getOrderNumber())
                                        .build();
                        loyaltyTransactionRepository.save(earnTx);
                }

                customer.setTotalSpent(customer.getTotalSpent().add(savedOrder.getFinalAmount()));
                customerRepository.save(customer);

                // 9. Process Payment Record
                Payment payment = Payment.builder()
                                .order(savedOrder)
                                .paymentMethod(request.getPaymentMethod())
                                .paymentStatus(PaymentStatus.PENDING)
                                .amount(savedOrder.getFinalAmount())
                                .build();
                paymentRepository.save(payment);

                // 10. Map Items and Return Detailed Response
                java.util.List<OrderResponse.OrderItemResponse> itemResponses = savedOrder.getItems().stream()
                                .map(item -> OrderResponse.OrderItemResponse.builder()
                                                .itemName(item.getItemName())
                                                .quantity(item.getQuantity())
                                                .unitPrice(item.getUnitPrice())
                                                .subtotal(item.getSubtotal())
                                                .build())
                                .collect(java.util.stream.Collectors.toList());

                return OrderResponse.builder()
                                .orderId(savedOrder.getId())
                                .orderNumber(savedOrder.getOrderNumber())
                                .orderStatus(savedOrder.getStatus().name())
                                .paymentStatus(payment.getPaymentStatus().name())
                                // Financials
                                .subtotal(savedOrder.getTotalAmount())
                                .taxAmount(savedOrder.getTaxAmount())
                                .deliveryFee(savedOrder.getDeliveryFee())
                                .serviceCharge(savedOrder.getServiceCharge())
                                .totalDiscountAmount(savedOrder.getDiscountAmount())
                                .finalTotal(savedOrder.getFinalAmount())
                                // Rewards & Coupons
                                .appliedCouponCode(savedOrder.getAppliedCouponCode())
                                .rewardPointsRedeemed(savedOrder.getRewardPointsRedeemed())
                                .rewardPointsEarned(savedOrder.getRewardPointsEarned())
                                // Metadata & Items
                                .createdAt(savedOrder.getCreatedAt())
                                .items(itemResponses)
                                .build();
        }

        @Override
        @Transactional
        public void updatePaymentStatus(Long orderId, PaymentUpdateRequest request) {
                // 1. Find the Order
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

                // 2. Find the Payment attached to this Order
                Payment payment = paymentRepository.findByOrder(order)
                                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found"));

                // 3. Update the fields based on the React payload
                payment.setPaymentStatus(PaymentStatus.valueOf(request.getPaymentStatus().toUpperCase()));
                order.setPaymentStatus(PaymentStatus.valueOf(request.getPaymentStatus().toUpperCase()));

                if (request.getTransactionId() != null) {
                        payment.setTransactionReference(request.getTransactionId());
                }

                // 4. If the payment is completed, stamp the exact time
                if ("PAID".equalsIgnoreCase(request.getPaymentStatus())) {
                        payment.setPaidAt(LocalDateTime.now());
                }

                // 5. Save both the payment AND the order back to the database
                paymentRepository.save(payment);
                orderRepository.save(order);
        }
}