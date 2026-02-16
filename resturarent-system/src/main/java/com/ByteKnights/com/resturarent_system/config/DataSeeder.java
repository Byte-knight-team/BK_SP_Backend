package com.ByteKnights.com.resturarent_system.config;

import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class DataSeeder implements CommandLineRunner {

        private final BranchRepository branchRepository;
        private final CustomerRepository customerRepository;
        private final OrderRepository orderRepository;
        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final MenuItemRepository menuItemRepository;
        private final OrderItemRepository orderItemRepository;

        public DataSeeder(BranchRepository branchRepository,
                        CustomerRepository customerRepository,
                        OrderRepository orderRepository,
                        UserRepository userRepository,
                        RoleRepository roleRepository,
                        MenuItemRepository menuItemRepository,
                        OrderItemRepository orderItemRepository) {
                this.branchRepository = branchRepository;
                this.customerRepository = customerRepository;
                this.orderRepository = orderRepository;
                this.userRepository = userRepository;
                this.roleRepository = roleRepository;
                this.menuItemRepository = menuItemRepository;
                this.orderItemRepository = orderItemRepository;
        }

        @Override
        @Transactional
        public void run(String... args) throws Exception {
                if (orderRepository.count() > 0) {
                        return;
                }

                System.out.println("Seeding data...");

                // Create Role
                Role customerRole = roleRepository.findByName("ROLE_CUSTOMER").orElseGet(() -> {
                        Role role = Role.builder()
                                        .name("ROLE_CUSTOMER")
                                        .description("Customer Role")
                                        .build();
                        return roleRepository.save(role);
                });

                // Create Branch
                Branch branch = Branch.builder()
                                .name("Main Branch")
                                .address("123 Food St")
                                .contactNumber("0112345678")
                                .email("main@kitchen.com")
                                .status(BranchStatus.ACTIVE)
                                .build();
                branch = branchRepository.save(branch);

                // Create User for Customer
                User user = User.builder()
                                .username("john_doe")
                                .email("john@example.com")
                                .password("password123") // In real app, encode this
                                .phone("0771234567")
                                .role(customerRole)
                                .build();
                user = userRepository.save(user);

                // Create Customer
                Customer customer = Customer.builder()
                                .user(user)
                                .loyaltyPoints(100)
                                .totalSpent(BigDecimal.ZERO)
                                .build();
                customer = customerRepository.save(customer);

                // Create Menu Items
                MenuItem burger = MenuItem.builder()
                                .branch(branch)
                                .name("Chicken Burger")
                                .description("Grilled chicken patty with fresh lettuce")
                                .price(BigDecimal.valueOf(15.99))
                                .category("Main Course")
                                .imageUrl("https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500")
                                .isAvailable(true)
                                .preparationTime(15)
                                .build();
                menuItemRepository.save(burger);

                MenuItem fries = MenuItem.builder()
                                .branch(branch)
                                .name("French Fries")
                                .description("Crispy salted fries")
                                .price(BigDecimal.valueOf(5.99))
                                .category("Sides")
                                .imageUrl("https://images.unsplash.com/photo-1573080496987-8198cb147a81?w=500")
                                .isAvailable(true)
                                .preparationTime(10)
                                .build();
                menuItemRepository.save(fries);

                // Create Order 1 (Pending)
                Order order1 = Order.builder()
                                .orderNumber("ORD-1204") // Matching frontend mock ID format
                                .branch(branch)
                                .customer(customer)
                                .orderType(OrderType.QR)
                                .status(OrderStatus.PLACED) // Mapping PLACED to 'pending' in frontend?
                                .totalAmount(BigDecimal.valueOf(21.98))
                                .finalAmount(BigDecimal.valueOf(21.98))
                                .paymentStatus(PaymentStatus.PENDING)
                                .build();
                order1 = orderRepository.save(order1);

                // Add items to Order 1
                OrderItem item1 = OrderItem.builder()
                                .order(order1)
                                .menuItem(burger)
                                .quantity(1)
                                .unitPrice(burger.getPrice())
                                .subtotal(burger.getPrice())
                                .build();
                orderItemRepository.save(item1);

                OrderItem item2 = OrderItem.builder()
                                .order(order1)
                                .menuItem(fries)
                                .quantity(1)
                                .unitPrice(fries.getPrice())
                                .subtotal(fries.getPrice())
                                .build();
                orderItemRepository.save(item2);

                // Create Order 2 (Preparing)
                Order order2 = Order.builder()
                                .orderNumber("ORD-1200")
                                .branch(branch)
                                .customer(customer)
                                .orderType(OrderType.QR)
                                .status(OrderStatus.PREPARING)
                                .totalAmount(BigDecimal.valueOf(31.98)) // 2 Burgers
                                .finalAmount(BigDecimal.valueOf(31.98))
                                .paymentStatus(PaymentStatus.PAID)
                                .build();
                order2 = orderRepository.save(order2);

                OrderItem item3 = OrderItem.builder()
                                .order(order2)
                                .menuItem(burger)
                                .quantity(2)
                                .unitPrice(burger.getPrice())
                                .subtotal(burger.getPrice().multiply(BigDecimal.valueOf(2)))
                                .build();
                orderItemRepository.save(item3);

                System.out.println("Data seeding completed.");
        }
}
