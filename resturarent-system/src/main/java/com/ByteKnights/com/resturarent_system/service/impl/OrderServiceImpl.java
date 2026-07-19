package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.customer.CheckoutCalculateRequest;
import com.ByteKnights.com.resturarent_system.dto.request.customer.PaymentUpdateRequest;
import com.ByteKnights.com.resturarent_system.dto.request.customer.PlaceOrderRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CheckoutCalculateResponse;
import com.ByteKnights.com.resturarent_system.dto.response.customer.BranchDetailResponse;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CustomerOrdersPageResponse;
import com.ByteKnights.com.resturarent_system.dto.response.customer.OrderPlacementResponse;
import com.ByteKnights.com.resturarent_system.dto.response.customer.OrderResponse;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.exception.CheckoutException;
import com.ByteKnights.com.resturarent_system.exception.ResourceNotFoundException;
import com.ByteKnights.com.resturarent_system.repository.*;
import java.util.HashMap;
import java.util.Map;
import com.ByteKnights.com.resturarent_system.service.CheckoutService;
import com.ByteKnights.com.resturarent_system.service.OrderService;
import com.ByteKnights.com.resturarent_system.service.QrSessionService;
import com.ByteKnights.com.resturarent_system.service.WebSocketNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.*;
import com.ByteKnights.com.resturarent_system.service.SystemConfigService;
import com.ByteKnights.com.resturarent_system.dto.cache.BranchConfigCacheDto;


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
        private final MenuItemIngredientRepository menuItemIngredientRepository;
        private final InventoryItemRepository inventoryItemRepository;
        private final InventoryTransactionRepository inventoryTransactionRepository;
        private final WebSocketNotificationService webSocketNotificationService;
        private final ReservationRepository reservationRepository;
        private final SystemConfigService systemConfigService;
        private final com.ByteKnights.com.resturarent_system.service.email.EmailService emailService;
        private final com.ByteKnights.com.resturarent_system.service.email.EmailTemplateService emailTemplateService;

        public OrderServiceImpl(CheckoutService checkoutService, QrSessionService qrSessionService,
                        OrderRepository orderRepository,
                        PaymentRepository paymentRepository, CustomerRepository customerRepository,
                        UserRepository userRepository, BranchRepository branchRepository,
                        RestaurantTableRepository tableRepository, MenuItemRepository menuItemRepository,
                        CouponRepository couponRepository, CouponUsageRepository couponUsageRepository,
                        LoyaltyTransactionRepository loyaltyTransactionRepository,
                        MenuItemIngredientRepository menuItemIngredientRepository,
                        InventoryItemRepository inventoryItemRepository,
                        InventoryTransactionRepository inventoryTransactionRepository,
                        WebSocketNotificationService webSocketNotificationService,
                        ReservationRepository reservationRepository,
                        SystemConfigService systemConfigService,
                        com.ByteKnights.com.resturarent_system.service.email.EmailService emailService,
                        com.ByteKnights.com.resturarent_system.service.email.EmailTemplateService emailTemplateService) {
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
                this.menuItemIngredientRepository = menuItemIngredientRepository;
                this.inventoryItemRepository = inventoryItemRepository;
                this.inventoryTransactionRepository = inventoryTransactionRepository;
                this.webSocketNotificationService = webSocketNotificationService;
                this.reservationRepository = reservationRepository;
                this.systemConfigService = systemConfigService;
                this.emailService = emailService;
                this.emailTemplateService = emailTemplateService;
        }

        @Override
        @Transactional
        public OrderPlacementResponse placeCustomerOrder(String userIdentifier, PlaceOrderRequest request) {

                // 1. Resolve Customer
                User user = userRepository.findByEmail(userIdentifier)
                                .orElseGet(() -> userRepository.findByPhone(userIdentifier)
                                                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
                Customer customer = customerRepository.findByUser(user)
                                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

                // 2. Resolve Branch & Table
                Branch branch = branchRepository.findById(request.getBranchId())
                                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
                                
                if (branch.getStatus() != BranchStatus.ACTIVE) {
                        throw new CheckoutException(HttpStatus.BAD_REQUEST, "This branch is currently closed and not accepting orders.");
                }

                BranchConfigCacheDto branchConfig = systemConfigService.getCachedBranchConfig(request.getBranchId());

                if ("ONLINE_DELIVERY".equals(request.getOrderType())) {
                        if (request.getLatitude() == null || request.getLongitude() == null) {
                                throw new CheckoutException(HttpStatus.BAD_REQUEST, "Delivery location coordinates are required.");
                        }
                        if (branch.getLatitude() == null || branch.getLongitude() == null) {
                                throw new CheckoutException(HttpStatus.INTERNAL_SERVER_ERROR, "Branch location is not configured properly.");
                        }
                        double distance = com.ByteKnights.com.resturarent_system.util.DistanceUtil.calculateDistance(
                                branch.getLatitude(), branch.getLongitude(),
                                request.getLatitude(), request.getLongitude()
                        );
                        if (distance > branchConfig.getMaxDeliveryRadiusKm()) {
                                throw new CheckoutException(HttpStatus.BAD_REQUEST, 
                                        String.format("Delivery location is outside our service area. Max range is %.1f km, but you are %.1f km away.", 
                                                branchConfig.getMaxDeliveryRadiusKm(), distance));
                        }
                }

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

                        // Check if table is occupied
                        if (table.getState() != TableStatus.OCCUPIED) {
                                throw new CheckoutException(HttpStatus.FORBIDDEN, 
                                        "Orders can only be placed when the table is marked as OCCUPIED. Please wait for a staff member to seat you.");
                        }

                        if (table.getSeatedReservationId() != null) {
                                Reservation activeReservation = reservationRepository.findById(table.getSeatedReservationId())
                                                .orElseThrow(() -> new ResourceNotFoundException("Active reservation not found"));
                                                
                                if (!activeReservation.getCustomer().getId().equals(customer.getId())) {
                                        throw new CheckoutException(HttpStatus.FORBIDDEN, 
                                                "This table is currently reserved by another customer. Only the reservation holder can place orders.");
                                }
                        }
                }

                // Batch-load menu items and ingredients to prevent N+1 queries
                List<Long> requestedItemIds = request.getItems().stream()
                        .map(PlaceOrderRequest.PlaceOrderItemRequest::getMenuItemId)
                        .collect(Collectors.toList());
                        
                List<MenuItem> dbMenuItems = menuItemRepository.findAllById(requestedItemIds);
                Map<Long, MenuItem> menuItemMap = dbMenuItems.stream()
                        .collect(Collectors.toMap(MenuItem::getId, item -> item));
                        
                List<MenuItemIngredient> allIngredients = menuItemIngredientRepository.findByMenuItemIdIn(requestedItemIds);
                Map<Long, List<MenuItemIngredient>> ingredientMap = allIngredients.stream()
                        .collect(Collectors.groupingBy(ing -> ing.getMenuItem().getId()));

                // 2.5 Pre-Order Inventory Validation & Aggregation
                Map<Long, BigDecimal> requiredIngredients = new HashMap<>();
                Map<Long, InventoryItem> inventoryItemCache = new HashMap<>();
                
                for (PlaceOrderRequest.PlaceOrderItemRequest itemReq : request.getItems()) {
                        MenuItem dbItem = menuItemMap.get(itemReq.getMenuItemId());
                        if (dbItem == null) {
                            throw new ResourceNotFoundException("Menu item not found");
                        }
                        if (Boolean.FALSE.equals(dbItem.getIsAvailable()) || dbItem.getStatus() != MenuItemStatus.ACTIVE) {
                            throw new CheckoutException(HttpStatus.BAD_REQUEST, dbItem.getName() + " is currently unavailable or inactive.");
                        }
                        if (!"ACTIVE".equals(dbItem.getCategory().getStatus())) {
                            throw new CheckoutException(HttpStatus.BAD_REQUEST, dbItem.getName() + " belongs to an inactive category.");
                        }
                                        
                        List<MenuItemIngredient> ingredients = ingredientMap.getOrDefault(dbItem.getId(), List.of());
                        
                        for (MenuItemIngredient ingredient : ingredients) {
                                InventoryItem invItem = ingredient.getInventoryItem();
                                if (!invItem.getBranch().getId().equals(branch.getId())) {
                                        continue; // Only check ingredients for this specific branch
                                }
                                
                                BigDecimal totalNeededForThisItem = ingredient.getQuantityRequired()
                                                .multiply(BigDecimal.valueOf(itemReq.getQuantity()));
                                                
                                requiredIngredients.merge(invItem.getId(), totalNeededForThisItem, BigDecimal::add);
                                inventoryItemCache.putIfAbsent(invItem.getId(), invItem);
                        }
                }
                
                // Validate stock levels
                for (Map.Entry<Long, BigDecimal> entry : requiredIngredients.entrySet()) {
                        InventoryItem invItem = inventoryItemCache.get(entry.getKey());
                        BigDecimal required = entry.getValue();
                        
                        if (invItem.getQuantity() == null || invItem.getQuantity().compareTo(required) < 0) {
                                throw new CheckoutException(HttpStatus.BAD_REQUEST, 
                                                "Insufficient stock to prepare this order.");
                        }
                }

                // 3. Call the CheckoutService to calculate the exact totals!
                CheckoutCalculateRequest calcRequest = new CheckoutCalculateRequest();
                calcRequest.setOrderType(request.getOrderType());
                calcRequest.setBranchId(request.getBranchId());
                calcRequest.setCouponCode(request.getCouponCode());
                calcRequest.setRedeemLoyaltyPoints(request.getRedeemLoyaltyPoints());
                calcRequest.setLatitude(request.getLatitude());
                calcRequest.setLongitude(request.getLongitude());
                calcRequest.setItems(request.getItems().stream()
                                .map(item -> {
                                        CheckoutCalculateRequest.CartItemRequest calcItem = new CheckoutCalculateRequest.CartItemRequest();
                                        calcItem.setMenuItemId(item.getMenuItemId());
                                        calcItem.setQuantity(item.getQuantity());
                                        return calcItem;
                                }).collect(Collectors.toList()));

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
                order.setLatitude(request.getLatitude());
                order.setLongitude(request.getLongitude());
                order.setKitchenNotes(request.getKitchenNotes());

                // Apply Math to Order
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
                for (PlaceOrderRequest.PlaceOrderItemRequest itemReq : request.getItems()) {
                        MenuItem dbItem = menuItemMap.get(itemReq.getMenuItemId());
                        OrderItem orderItem = new OrderItem();
                        orderItem.setMenuItem(dbItem);
                        orderItem.setItemName(dbItem.getName());
                        orderItem.setQuantity(itemReq.getQuantity());
                        orderItem.setUnitPrice(dbItem.getPrice());
                        orderItem.setSubtotal(dbItem.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
                        orderItem.setKitchenNotes(itemReq.getKitchenNote());
                        order.addItem(orderItem);
                }

                // Generate Order Number
                String uniqueSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                order.setOrderNumber("#ORD-" + uniqueSuffix);

                // 6. SAVE ORDER
                Order savedOrder = orderRepository.save(order);
                
                // 6.5 Deduct Inventory and Log Transactions
                for (Map.Entry<Long, BigDecimal> entry : requiredIngredients.entrySet()) {
                        InventoryItem invItem = inventoryItemCache.get(entry.getKey());
                        BigDecimal required = entry.getValue();
                        BigDecimal oldQuantity = invItem.getQuantity();
                        BigDecimal newQuantity = oldQuantity.subtract(required);
                        
                        invItem.setQuantity(newQuantity);
                        inventoryItemRepository.save(invItem);
                        
                        InventoryTransaction tx = InventoryTransaction.builder()
                                        .inventoryItem(invItem)
                                        .staff(null) // Automated system action
                                        .transactionType(InventoryTransactionType.ORDER_DEDUCT)
                                        .quantityChange(required.negate())
                                        .previousQuantity(oldQuantity)
                                        .newQuantity(newQuantity)
                                        .unitPrice(invItem.getUnitPrice())
                                        .notes("Automated deduction for order " + savedOrder.getOrderNumber())
                                        .build();
                        inventoryTransactionRepository.save(tx);
                }

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
                webSocketNotificationService.broadcastOrderStatusUpdate(savedOrder.getId(), savedOrder.getStatus().name());
                webSocketNotificationService.broadcastNewReceptionistOrder(
                        savedOrder.getBranch().getId(),
                        savedOrder.getOrderNumber(),
                        savedOrder.getOrderType().name(),
                        savedOrder.getId()
                );

                // 11. Send Order Placed Email (Async)
                if (customer.getUser() != null && customer.getUser().getEmail() != null) {
                        final String toEmail = customer.getUser().getEmail();
                        final String orderNum = savedOrder.getOrderNumber();
                        final String branchName = savedOrder.getBranch() != null ? savedOrder.getBranch().getName() : "Crave House";
                        final BigDecimal finalAmount = savedOrder.getFinalAmount();
                        final String typeStr = savedOrder.getOrderType().name();
                        final String itemsSummary = savedOrder.getItems().stream()
                                        .map(i -> i.getQuantity() + "x " + i.getItemName())
                                        .collect(Collectors.joining("\n"));
                        
                        java.util.concurrent.CompletableFuture.runAsync(() -> {
                                try {
                                        String html = emailTemplateService.buildOrderPlacedHtml(orderNum, branchName, itemsSummary, finalAmount, typeStr);
                                        emailService.sendHtmlEmail(toEmail, "Order Confirmed — " + orderNum, html);
                                } catch (Exception e) {
                                        // Ignore
                                }
                        });
                }

                return OrderPlacementResponse.builder()
                                .orderId(savedOrder.getId())
                                .orderNumber(savedOrder.getOrderNumber())
                                .finalAmount(savedOrder.getFinalAmount())
                                .build();
        }

        // helper method to convert order entity to response
        private OrderResponse mapToOrderResponse(Order order) {
                Payment payment = paymentRepository.findByOrder(order).orElse(null);

                java.util.List<OrderResponse.OrderItemResponse> itemResponses = order.getItems().stream()
                                .map(item -> OrderResponse.OrderItemResponse.builder()
                                                .orderItemId(item.getId())
                                                .menuItemId(item.getMenuItem() != null ? item.getMenuItem().getId()
                                                                : null)
                                                .itemName(item.getItemName())
                                                .quantity(item.getQuantity())
                                                .unitPrice(item.getUnitPrice())
                                                .subtotal(item.getSubtotal())
                                                .isReviewed(order.getReviews().stream().anyMatch(
                                                                r -> r.getOrderItem() != null && r.getOrderItem()
                                                                                .getId().equals(item.getId())))
                                                .kitchenNotes(item.getKitchenNotes())
                                                .build())
                                .collect(Collectors.toList());

                return OrderResponse.builder()
                                .orderId(order.getId())
                                .orderNumber(order.getOrderNumber())
                                .orderStatus(order.getStatus().name())
                                .orderType(order.getOrderType() != null ? order.getOrderType().name() : null)
                                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name()
                                                : PaymentStatus.PENDING.name())
                                .paymentMethod(payment != null && payment.getPaymentMethod() != null
                                                ? payment.getPaymentMethod().name()
                                                : null)
                                .cancellationReason(order.getCancelReason())
                                .isReviewed(order.getReviews().stream().anyMatch(r -> r.getOrderItem() == null))
                                .contactName(order.getContactName())
                                .contactPhone(order.getContactPhone())
                                .deliveryAddress(order.getDeliveryAddress())
                                .kitchenNotes(order.getKitchenNotes())
                                .tableId(order.getTable() != null ? order.getTable().getId() : null)
                                .tableNumber(order.getTable() != null ? order.getTable().getTableNumber() : null)
                                // Financials
                                .subtotal(order.getTotalAmount())
                                .taxAmount(order.getTaxAmount())
                                .deliveryFee(order.getDeliveryFee())
                                .serviceCharge(order.getServiceCharge())
                                .totalDiscountAmount(order.getDiscountAmount())
                                .finalTotal(order.getFinalAmount())
                                // Rewards & Coupons
                                .appliedCouponCode(order.getAppliedCouponCode())
                                .rewardPointsRedeemed(order.getRewardPointsRedeemed())
                                .rewardPointsEarned(order.getRewardPointsEarned())
                                // Metadata & Items
                                .createdAt(order.getCreatedAt())
                                .branchDetails(order.getBranch() != null ? BranchDetailResponse.builder()
                                                .name(order.getBranch().getName())
                                                .address(order.getBranch().getAddress())
                                                .contactNumber(order.getBranch().getContactNumber())
                                                .email(order.getBranch().getEmail())
                                                .build() : null)
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

        @Override
        @Transactional(readOnly = true)
        public List<OrderResponse> getCustomerOrders(String userIdentifier) {
                return getCustomerOrders(userIdentifier, null, null);
        }

        @Override
        @Transactional(readOnly = true)
        public List<OrderResponse> getCustomerOrders(String userIdentifier, String orderTypeFilter, Boolean isActive) {
                return getCustomerOrdersPage(userIdentifier, orderTypeFilter, isActive, 0, Integer.MAX_VALUE)
                                .getOrders();
        }

        @Override
        @Transactional(readOnly = true)
        public CustomerOrdersPageResponse getCustomerOrdersPage(String userIdentifier, String orderTypeFilter,
                        Boolean isActive, int page, int size) {
                Customer customer = customerRepository.findByUserPhone(userIdentifier)
                                .orElseGet(() -> customerRepository.findByUserEmail(userIdentifier)
                                                .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Customer not found")));
                // check if valid type assigned
                OrderType parsedType = null;
                if (orderTypeFilter != null && !orderTypeFilter.isEmpty() && !orderTypeFilter.equals("ALL")) {
                        try {
                                parsedType = OrderType.valueOf(orderTypeFilter.toUpperCase());
                        } catch (IllegalArgumentException e) {
                                // Ignore invalid type
                        }
                }

                int safePage = Math.max(page, 0);
                int safeSize = Math.max(size, 1);
                PageRequest pageRequest = PageRequest.of(safePage, safeSize);

                // get orders without type filter
                if (parsedType == null) {
                        Page<Order> orderPage = orderRepository.findFilteredOrdersWithoutType(customer.getId(),
                                        isActive, pageRequest);
                        return buildCustomerOrdersPage(orderPage, safePage, safeSize);
                        // get orders with type filter
                } else {
                        Page<Order> orderPage = orderRepository.findFilteredOrders(customer.getId(), parsedType,
                                        isActive, pageRequest);
                        return buildCustomerOrdersPage(orderPage, safePage, safeSize);
                }
        }

        private CustomerOrdersPageResponse buildCustomerOrdersPage(Page<Order> orderPage, int page, int size) {
                List<OrderResponse> orders = orderPage.getContent().stream()
                                .map(this::mapToOrderResponse)
                                .collect(Collectors.toList());

                return CustomerOrdersPageResponse.builder()
                                .orders(orders)
                                .page(page)
                                .size(size)
                                .totalElements(orderPage.getTotalElements())
                                .totalPages(orderPage.getTotalPages())
                                .last(orderPage.isLast())
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public OrderResponse getCustomerOrderById(String userIdentifier, Long orderId) {
                Customer customer = customerRepository.findByUserPhone(userIdentifier)
                                .orElseGet(() -> customerRepository.findByUserEmail(userIdentifier)
                                                .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Customer not found")));

                Order order = orderRepository.findByIdAndCustomerId(orderId, customer.getId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Order not found or does not belong to user"));

                return mapToOrderResponse(order);
        }

        @Override
        @Transactional
        public void cancelCustomerOrder(String userIdentifier, Long orderId, String cancelReason) {
                Customer customer = customerRepository.findByUserPhone(userIdentifier)
                                .orElseGet(() -> customerRepository.findByUserEmail(userIdentifier)
                                                .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Customer not found")));

                Order order = orderRepository.findByIdAndCustomerId(orderId, customer.getId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Order not found or does not belong to user"));

                // Ensure it's active
                if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.PENDING
                                && order.getStatus() != OrderStatus.ON_HOLD) {
                        throw new CheckoutException(HttpStatus.BAD_REQUEST,
                                        "Cannot cancel an order that is already preparing, completed, or cancelled.");
                }

                // Rollback loyalty points
                if (!loyaltyTransactionRepository.existsByOrderAndTransactionType(order,
                                LoyaltyTransactionType.REFUND)) {
                        int currentPoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
                        int redeemedPoints = order.getRewardPointsRedeemed() != null ? order.getRewardPointsRedeemed()
                                        : 0;
                        int earnedPoints = order.getRewardPointsEarned() != null ? order.getRewardPointsEarned() : 0;

                        if (redeemedPoints > 0) {
                                customer.setLoyaltyPoints(currentPoints + redeemedPoints);
                                currentPoints = customer.getLoyaltyPoints();

                                LoyaltyTransaction refundRedeemedTx = LoyaltyTransaction.builder()
                                                .customer(customer)
                                                .order(order)
                                                .transactionType(LoyaltyTransactionType.REFUND)
                                                .points(redeemedPoints)
                                                .description("Refunded redeemed points for cancelled order "
                                                                + order.getOrderNumber())
                                                .build();
                                loyaltyTransactionRepository.save(refundRedeemedTx);
                        }

                        if (earnedPoints > 0) {
                                int pointsToReverse = Math.min(currentPoints, earnedPoints);
                                customer.setLoyaltyPoints(Math.max(0, currentPoints - pointsToReverse));

                                LoyaltyTransaction reverseEarnedTx = LoyaltyTransaction.builder()
                                                .customer(customer)
                                                .order(order)
                                                .transactionType(LoyaltyTransactionType.REFUND)
                                                .points(-pointsToReverse)
                                                .description("Reversed earned points for cancelled order "
                                                                + order.getOrderNumber())
                                                .build();
                                loyaltyTransactionRepository.save(reverseEarnedTx);
                        }
                }
                // restock used coupon and delete usage record
                if (order.getAppliedCouponCode() != null && !order.getAppliedCouponCode().isBlank()) {
                        couponUsageRepository.findByOrder(order).ifPresent(couponUsageRepository::delete);

                        couponRepository.findByCode(order.getAppliedCouponCode()).ifPresent(coupon -> {
                                int usedCount = coupon.getUsedCount() != null ? coupon.getUsedCount() : 0;
                                coupon.setUsedCount(Math.max(0, usedCount - 1));
                                couponRepository.save(coupon);
                        });
                }

                if (order.getFinalAmount() != null && customer.getTotalSpent() != null) {
                        customer.setTotalSpent(
                                        customer.getTotalSpent().subtract(order.getFinalAmount()).max(BigDecimal.ZERO));
                }

                customerRepository.save(customer);

                // Refund inventory
                Map<Long, BigDecimal> refundIngredients = new HashMap<>();
                
                // Batch load ingredients
                List<Long> menuItemIds = order.getItems().stream()
                        .filter(item -> item.getMenuItem() != null)
                        .map(item -> item.getMenuItem().getId())
                        .collect(Collectors.toList());
                        
                if (!menuItemIds.isEmpty()) {
                        List<MenuItemIngredient> allIngredients = menuItemIngredientRepository.findByMenuItemIdIn(menuItemIds);
                        Map<Long, List<MenuItemIngredient>> ingredientMap = allIngredients.stream()
                                .collect(Collectors.groupingBy(ing -> ing.getMenuItem().getId()));
                        
                        for (OrderItem item : order.getItems()) {
                                if (item.getMenuItem() != null) {
                                        List<MenuItemIngredient> ingredients = ingredientMap.getOrDefault(item.getMenuItem().getId(), List.of());
                                        for (MenuItemIngredient ingredient : ingredients) {
                                                InventoryItem invItem = ingredient.getInventoryItem();
                                                if (invItem.getBranch().getId().equals(order.getBranch().getId())) {
                                                        BigDecimal amountToRefund = ingredient.getQuantityRequired()
                                                                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                                                        refundIngredients.merge(invItem.getId(), amountToRefund, BigDecimal::add);
                                                }
                                        }
                                }
                        }
                }

                // Batch load inventory items to refund
                if (!refundIngredients.isEmpty()) {
                        List<InventoryItem> invItems = inventoryItemRepository.findAllById(refundIngredients.keySet());
                        for (InventoryItem invItem : invItems) {
                                BigDecimal refundAmount = refundIngredients.get(invItem.getId());
                                BigDecimal oldQuantity = invItem.getQuantity();
                                BigDecimal newQuantity = oldQuantity.add(refundAmount);
                                
                                invItem.setQuantity(newQuantity);
                                inventoryItemRepository.save(invItem);
                                
                                InventoryTransaction tx = InventoryTransaction.builder()
                                                .inventoryItem(invItem)
                                                .staff(null)
                                                .transactionType(InventoryTransactionType.ORDER_REFUND)
                                                .quantityChange(refundAmount)
                                                .previousQuantity(oldQuantity)
                                                .newQuantity(newQuantity)
                                                .unitPrice(invItem.getUnitPrice())
                                                .notes("Automated refund for cancelled order " + order.getOrderNumber())
                                                .build();
                                inventoryTransactionRepository.save(tx);
                        }
                }

                order.setStatus(OrderStatus.CANCELLED);
                order.setCancelReason(cancelReason);
                orderRepository.save(order);
                webSocketNotificationService.broadcastOrderStatusUpdate(order.getId(), order.getStatus().name());

                // Send Cancelled Email (Async)
                if (customer.getUser() != null && customer.getUser().getEmail() != null) {
                        final String toEmail = customer.getUser().getEmail();
                        final String orderNum = order.getOrderNumber();
                        final String branchName = order.getBranch() != null ? order.getBranch().getName() : "Crave House";
                        final String finalReason = cancelReason != null ? cancelReason : "Cancelled by customer";
                        
                        java.util.concurrent.CompletableFuture.runAsync(() -> {
                                try {
                                        String html = emailTemplateService.buildOrderCancelledHtml(orderNum, branchName, finalReason);
                                        emailService.sendHtmlEmail(toEmail, "Order Cancelled — " + orderNum, html);
                                } catch (Exception e) {
                                        // Ignore
                                }
                        });
                }
        }
}