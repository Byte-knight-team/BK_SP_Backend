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
        private final InventoryItemRepository inventoryItemRepository;
        private final ChefRequestRepository chefRequestRepository;

        public DataSeeder(BranchRepository branchRepository,
                        CustomerRepository customerRepository,
                        OrderRepository orderRepository,
                        UserRepository userRepository,
                        RoleRepository roleRepository,
                        MenuItemRepository menuItemRepository,
                        OrderItemRepository orderItemRepository,
                        InventoryItemRepository inventoryItemRepository,
                        ChefRequestRepository chefRequestRepository) {
                this.branchRepository = branchRepository;
                this.customerRepository = customerRepository;
                this.orderRepository = orderRepository;
                this.userRepository = userRepository;
                this.roleRepository = roleRepository;
                this.menuItemRepository = menuItemRepository;
                this.orderItemRepository = orderItemRepository;
                this.inventoryItemRepository = inventoryItemRepository;
                this.chefRequestRepository = chefRequestRepository;
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
                                .imageUrl(null)
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
                                .imageUrl(null)
                                .isAvailable(true)
                                .preparationTime(10)
                                .build();
                menuItemRepository.save(fries);

                // Create Order 1 (Pending) - 9:00 AM
                Order order1 = Order.builder()
                                .orderNumber("ORD-1204") // Matching frontend mock ID format
                                .branch(branch)
                                .customer(customer)
                                .orderType(OrderType.QR)
                                .status(OrderStatus.PLACED) // Mapping PLACED to 'pending' in frontend?
                                .totalAmount(BigDecimal.valueOf(21.98))
                                .finalAmount(BigDecimal.valueOf(21.98))
                                .paymentStatus(PaymentStatus.PENDING)
                                .createdAt(java.time.LocalDateTime.of(java.time.LocalDate.now(),
                                                java.time.LocalTime.of(9, 0)))
                                .build();
                order1 = orderRepository.save(order1);

                // Add items to Order 1
                OrderItem item1 = OrderItem.builder()
                                .order(order1)
                                .menuItem(burger)
                                .itemName(burger.getName())
                                .quantity(1)
                                .unitPrice(burger.getPrice())
                                .subtotal(burger.getPrice())
                                .build();
                orderItemRepository.save(item1);

                OrderItem item2 = OrderItem.builder()
                                .order(order1)
                                .menuItem(fries)
                                .itemName(fries.getName())
                                .quantity(1)
                                .unitPrice(fries.getPrice())
                                .subtotal(fries.getPrice())
                                .build();
                orderItemRepository.save(item2);

                // Create Order 2 (Preparing) - 9:30 AM
                Order order2 = Order.builder()
                                .orderNumber("ORD-1200")
                                .branch(branch)
                                .customer(customer)
                                .orderType(OrderType.QR)
                                .status(OrderStatus.PLACED)
                                .totalAmount(BigDecimal.valueOf(37.97)) // 2 Burgers + 1 Fries
                                .finalAmount(BigDecimal.valueOf(37.97))
                                .paymentStatus(PaymentStatus.PAID)
                                .createdAt(java.time.LocalDateTime.of(java.time.LocalDate.now(),
                                                java.time.LocalTime.of(9, 30)))
                                .build();
                order2 = orderRepository.save(order2);

                OrderItem item3 = OrderItem.builder()
                                .order(order2)
                                .menuItem(burger)
                                .itemName(burger.getName())
                                .quantity(2)
                                .unitPrice(burger.getPrice())
                                .subtotal(burger.getPrice().multiply(BigDecimal.valueOf(2)))
                                .build();
                orderItemRepository.save(item3);

                OrderItem item3_2 = OrderItem.builder()
                                .order(order2)
                                .menuItem(fries)
                                .itemName(fries.getName())
                                .quantity(1)
                                .unitPrice(fries.getPrice())
                                .subtotal(fries.getPrice())
                                .build();
                orderItemRepository.save(item3_2);

                System.out.println("Seeding completed for Order 1 & 2.");

                // Create Order 3 (Pending) - 11:00 AM
                Order order3 = Order.builder()
                                .orderNumber("ORD-1205")
                                .branch(branch)
                                .customer(customer)
                                .orderType(OrderType.ONLINE)
                                .status(OrderStatus.PLACED)
                                .totalAmount(fries.getPrice())
                                .finalAmount(fries.getPrice())
                                .paymentStatus(PaymentStatus.PENDING)
                                .createdAt(java.time.LocalDateTime.of(java.time.LocalDate.now(),
                                                java.time.LocalTime.of(11, 0)))
                                .build();
                order3 = orderRepository.save(order3);

                OrderItem item4 = OrderItem.builder()
                                .order(order3)
                                .menuItem(fries)
                                .itemName(fries.getName())
                                .quantity(1)
                                .unitPrice(fries.getPrice())
                                .subtotal(fries.getPrice())
                                .build();
                orderItemRepository.save(item4);

                System.out.println("Data seeding completed.");

                // Seed Inventory Items
                System.out.println("Seeding Inventory Items...");

                // Item 1: Good Stock
                InventoryItem inventoryItem1 = InventoryItem.builder()
                                .branch(branch)
                                .name("Premium Pizza Flour")
                                .category("Grains")
                                .unitPrice(new BigDecimal("12.50"))
                                .unit("kg")
                                .quantity(new BigDecimal("50.00")) // Current Stock
                                .reorderLevel(new BigDecimal("15.00")) // Low Stock Threshold
                                .build();
                inventoryItemRepository.save(inventoryItem1);

                // Item 2: Warning Status (Quantity <= ReorderLevel)
                InventoryItem inventoryItem2 = InventoryItem.builder()
                                .branch(branch)
                                .name("Mozzarella Cheese")
                                .category("Dairy")
                                .unitPrice(new BigDecimal("24.00"))
                                .unit("kg")
                                .quantity(new BigDecimal("8.00")) // Current Stock (WARNING!)
                                .reorderLevel(new BigDecimal("10.00")) // Low Stock Threshold
                                .build();
                inventoryItemRepository.save(inventoryItem2);

                // Item 3: Good Stock (Pcs)
                InventoryItem inventoryItem3 = InventoryItem.builder()
                                .branch(branch)
                                .name("Pizza Boxes (Large)")
                                .category("Packaging")
                                .unitPrice(new BigDecimal("0.50"))
                                .unit("Pcs")
                                .quantity(new BigDecimal("500.00"))
                                .reorderLevel(new BigDecimal("100.00"))
                                .build();
                inventoryItemRepository.save(inventoryItem3);

                // Seed Chef Requests
                System.out.println("Seeding Chef Requests...");

                // Request 1: Pending
                ChefRequest req1 = ChefRequest.builder()
                                .branch(branch)
                                .chefName("Chef Adikaram")
                                .chefRole("EXECUTIVE CHEF")
                                .itemName("Olive Oil")
                                .requestedQuantity(new BigDecimal("10.00"))
                                .unit("Liters")
                                .chefNote("Running very low for weekend rush.")
                                .status(com.ByteKnights.com.resturarent_system.entity.ChefRequestStatus.PENDING)
                                .build();
                chefRequestRepository.save(req1);

                // Request 2: Pending
                ChefRequest req2 = ChefRequest.builder()
                                .branch(branch)
                                .chefName("Chef Silva")
                                .chefRole("SOUS CHEF")
                                .itemName("Fresh Basil")
                                .requestedQuantity(new BigDecimal("2.00"))
                                .unit("kg")
                                .chefNote("Supplier missed last delivery.")
                                .status(com.ByteKnights.com.resturarent_system.entity.ChefRequestStatus.PENDING)
                                .build();
                chefRequestRepository.save(req2);

                // Request 3: Pending
                ChefRequest req3 = ChefRequest.builder()
                                .branch(branch)
                                .chefName("Chef Kumara")
                                .chefRole("SOUS CHEF")
                                .itemName("Chicken")
                                .requestedQuantity(new BigDecimal("4.00"))
                                .unit("kg")
                                .chefNote("Running low for high demand.")
                                .status(com.ByteKnights.com.resturarent_system.entity.ChefRequestStatus.PENDING)
                                .build();
                chefRequestRepository.save(req3);

        }
}
