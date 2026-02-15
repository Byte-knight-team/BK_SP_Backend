package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.CreateOrderRequest;
import com.ByteKnights.com.resturarent_system.dto.OrderResponse;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final BranchRepository branchRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * Creates a new online order.
     *
     * Flow:
     *   1. Validate branch exists
     *   2. Find or create a guest customer (since auth isn't wired yet)
     *   3. Resolve each menu item, verify availability, snapshot the price
     *   4. Calculate totals
     *   5. Persist order + items
     *   6. Return OrderResponse DTO
     */
    @Transactional
    public OrderResponse createOnlineOrder(CreateOrderRequest request) {

        // ── 1. Look up the branch ──
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + request.getBranchId()));

        // ── 2. Find or create guest customer ──
        Customer customer = findOrCreateGuestCustomer(request.getCustomerName(), request.getCustomerPhone());

        // ── 3. Generate unique order number ──
        String orderNumber = generateOrderNumber();

        // ── 4. Map payment method ──
        PaymentMethod paymentMethod = "pay-now".equalsIgnoreCase(request.getPaymentMethod())
                ? PaymentMethod.ONLINE
                : PaymentMethod.CASH;

        // ── 5. Build the Order entity ──
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .branch(branch)
                .customer(customer)
                .orderType(OrderType.ONLINE)
                .status(OrderStatus.PLACED)
                .paymentStatus(PaymentStatus.PENDING)
                .discountAmount(BigDecimal.ZERO)
                .build();

        // ── 6. Resolve menu items, create OrderItems, calculate totals ──
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            MenuItem menuItem = menuItemRepository.findById(itemReq.getMenuItemId())
                    .orElseThrow(() -> new RuntimeException("Menu item not found: " + itemReq.getMenuItemId()));

            if (!menuItem.getIsAvailable()) {
                throw new RuntimeException("Menu item is not available: " + menuItem.getName());
            }

            BigDecimal unitPrice = menuItem.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .menuItem(menuItem)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();

            order.addItem(orderItem);
            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);
        order.setFinalAmount(totalAmount.subtract(order.getDiscountAmount()));

        // ── 7. Save ──
        Order savedOrder = orderRepository.save(order);

        // ── 8. Build response ──
        return toOrderResponse(savedOrder, request);
    }

    // ─────────────────── Helper: find/create guest customer ───────────────────
    private Customer findOrCreateGuestCustomer(String name, String phone) {
        // Check if a user with this phone already exists
        return userRepository.findByPhone(phone)
                .map(user -> customerRepository.findByUser(user)
                        .orElseGet(() -> {
                            Customer c = Customer.builder().user(user).build();
                            return customerRepository.save(c);
                        }))
                .orElseGet(() -> {
                    // Create a guest user + customer record
                    Role customerRole = roleRepository.findByName("CUSTOMER")
                            .orElseGet(() -> roleRepository.save(
                                    Role.builder().name("CUSTOMER").description("Customer role").build()
                            ));

                    User guestUser = User.builder()
                            .username(name)
                            .password("GUEST_NO_PASSWORD")  // placeholder — no login for guests
                            .phone(phone)
                            .role(customerRole)
                            .isActive(true)
                            .build();
                    guestUser = userRepository.save(guestUser);

                    Customer customer = Customer.builder().user(guestUser).build();
                    return customerRepository.save(customer);
                });
    }

    // ─────────────────── Helper: generate order number ───────────────────
    private String generateOrderNumber() {
        // Format: ORD-<6 digit random> e.g. ORD-847291
        return "ORD-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    // ─────────────────── Helper: entity → DTO ───────────────────
    private OrderResponse toOrderResponse(Order order, CreateOrderRequest request) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderResponse.OrderItemResponse.builder()
                        .menuItemId(item.getMenuItem().getId())
                        .menuItemName(item.getMenuItem().getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .orderType(request.getOrderType())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus().name())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .deliveryAddress(request.getDeliveryAddress())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}
