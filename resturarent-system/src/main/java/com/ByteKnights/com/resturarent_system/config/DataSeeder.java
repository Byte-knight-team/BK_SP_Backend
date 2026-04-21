package com.ByteKnights.com.resturarent_system.config;

import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Component
public class DataSeeder implements CommandLineRunner {

        private final BranchRepository branchRepository;
        private final CustomerRepository customerRepository;
        private final OrderRepository orderRepository;
        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final MenuCategoryRepository menuCategoryRepository;
        private final MenuItemRepository menuItemRepository;
        private final OrderItemRepository orderItemRepository;
        private final InventoryItemRepository inventoryItemRepository;

        public DataSeeder(BranchRepository branchRepository,
                          CustomerRepository customerRepository,
                          OrderRepository orderRepository,
                          UserRepository userRepository,
                          RoleRepository roleRepository,
                          MenuCategoryRepository menuCategoryRepository,
                          MenuItemRepository menuItemRepository,
                          OrderItemRepository orderItemRepository,
                          InventoryItemRepository inventoryItemRepository) {
                this.branchRepository = branchRepository;
                this.customerRepository = customerRepository;
                this.orderRepository = orderRepository;
                this.userRepository = userRepository;
                this.roleRepository = roleRepository;
                this.menuCategoryRepository = menuCategoryRepository;
                this.menuItemRepository = menuItemRepository;
                this.orderItemRepository = orderItemRepository;
                this.inventoryItemRepository = inventoryItemRepository;
        }

        @Override
        @Transactional
        public void run(String... args) throws Exception {
                // පරණ දත්ත ඉවත් කිරීම
                orderItemRepository.deleteAll();
                orderRepository.deleteAll();
                inventoryItemRepository.deleteAll();

                System.out.println("=================================================");
                System.out.println("Starting to seed database for Kitchen Dashboard...");

                // 1. Create Role, Branch, User, and Customer
                Role customerRole = roleRepository.findByName("ROLE_CUSTOMER").orElseGet(() ->
                        roleRepository.save(Role.builder().name("ROLE_CUSTOMER").description("Customer Role").build())
                );

                Branch branch = branchRepository.findAll().stream().findFirst().orElseGet(() ->
                        branchRepository.save(Branch.builder()
                                .name("Main Branch")
                                .address("123 Food St")
                                .contactNumber("0112345678")
                                .email("main@kitchen.com")
                                .status(BranchStatus.ACTIVE)
                                .build())
                );

                User user = userRepository.findByUsername("john_doe").orElseGet(() ->
                        userRepository.save(User.builder()
                                .username("john_doe")
                                .email("john@example.com")
                                .password("password123")
                                .phone("0771234567")
                                .role(customerRole)
                                .build())
                );

                Customer customer = customerRepository.findAll().stream().findFirst().orElseGet(() ->
                        customerRepository.save(Customer.builder()
                                .user(user)
                                .loyaltyPoints(100)
                                .totalSpent(BigDecimal.ZERO)
                                .build())
                );

                // 2. Create Menu Items
                MenuCategory mainCourse = menuCategoryRepository.save(MenuCategory.builder().name("Main Course").build());

                MenuItem burger = menuItemRepository.save(MenuItem.builder()
                        .branch(branch).category(mainCourse).name("Chicken Burger")
                        .price(BigDecimal.valueOf(1500.00)).isAvailable(true)
                        .status(MenuItemStatus.APPROVED).preparationTime(15).build());

                MenuItem rice = menuItemRepository.save(MenuItem.builder()
                        .branch(branch).category(mainCourse).name("Mixed Fried Rice")
                        .price(BigDecimal.valueOf(1200.00)).isAvailable(true)
                        .status(MenuItemStatus.APPROVED).preparationTime(20).build());

                MenuItem kottu = menuItemRepository.save(MenuItem.builder()
                        .branch(branch).category(mainCourse).name("Chicken Kottu")
                        .price(BigDecimal.valueOf(1100.00)).isAvailable(true)
                        .status(MenuItemStatus.APPROVED).preparationTime(18).build());

                MenuItem pasta = menuItemRepository.save(MenuItem.builder()
                        .branch(branch).category(mainCourse).name("Pasta Carbonara")
                        .price(BigDecimal.valueOf(1800.00)).isAvailable(true)
                        .status(MenuItemStatus.APPROVED).preparationTime(25).build());

                MenuItem pizza = menuItemRepository.save(MenuItem.builder()
                        .branch(branch).category(mainCourse).name("Seafood Pizza")
                        .price(BigDecimal.valueOf(2500.00)).isAvailable(true)
                        .status(MenuItemStatus.APPROVED).preparationTime(30).build());

                LocalDate today = LocalDate.now();

                // 3. Seed Orders for Sorting Testing (FIFO/LIFO)
                // --- PENDING Orders (8AM oldest) ---
                createTestOrder(branch, customer, burger, OrderStatus.PENDING, today.atTime(8, 0));
                createTestOrder(branch, customer, pizza, OrderStatus.PENDING, today.atTime(10, 0));

                // --- PREPARING Orders (9AM oldest) ---
                createTestOrder(branch, customer, pasta, OrderStatus.PREPARING, today.atTime(9, 0));
                createTestOrder(branch, customer, rice, OrderStatus.PREPARING, today.atTime(11, 0));

                // --- COMPLETED Orders (History: 3PM newest) ---
                createTestOrder(branch, customer, kottu, OrderStatus.COMPLETED, today.atTime(13, 0));
                createTestOrder(branch, customer, burger, OrderStatus.COMPLETED, today.atTime(15, 0));

                // 4. Seed Inventory Items for Alerts
                createInventoryItem(branch, "Basmati Rice", 100, 80, 20, "KG");
                createInventoryItem(branch, "Maldon Sea Salt", 30, 10, 15, "KG");
                createInventoryItem(branch, "Truffle Oil", 10, 2, 6, "LITERS");
                createInventoryItem(branch, "Wagyu Beef (A5)", 20, 4, 10, "KG");

                System.out.println("✅ Data seeding successfully completed!");
                System.out.println("=================================================");
        }

        private void createTestOrder(Branch branch, Customer customer, MenuItem menuItem, OrderStatus status, LocalDateTime statusTime) {
                Order order = new Order();
                order.setBranch(branch);
                order.setCustomer(customer);
                order.setStatus(status);
                order.setOrderType(OrderType.QR);
                order.setStatusUpdatedAt(statusTime);
                order.setCreatedAt(statusTime.minusMinutes(30));
                order.setTotalAmount(menuItem.getPrice());
                order.setItems(new ArrayList<>()); // Initialize the list

                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setMenuItem(menuItem);
                item.setItemName(menuItem.getName());
                item.setQuantity(2);
                item.setUnitPrice(menuItem.getPrice());
                item.setSubtotal(menuItem.getPrice().multiply(BigDecimal.valueOf(2)));

                order.addItem(item);
                orderRepository.save(order);
        }

        private void createInventoryItem(Branch branch, String name, double max, double current, double reorder, String unit) {
                InventoryItem item = InventoryItem.builder()
                        .branch(branch)
                        .name(name)
                        .maxStock(BigDecimal.valueOf(max))
                        .quantity(BigDecimal.valueOf(current))
                        .reorderLevel(BigDecimal.valueOf(reorder))
                        .unit(unit)
                        .build();
                inventoryItemRepository.save(item);
        }
}
