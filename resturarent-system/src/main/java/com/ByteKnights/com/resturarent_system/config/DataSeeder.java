package com.ByteKnights.com.resturarent_system.config;

import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
                // Already seeded data delete karanna, nathnam thawa duplication wenawa
                orderItemRepository.deleteAll();
                orderRepository.deleteAll();

                System.out.println("=================================================");
                System.out.println("Starting to seed database for Kitchen Dashboard...");

                // 1. Create Role, User, and Customer
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

                LocalDateTime now = LocalDateTime.now();

                // ==============================================
                // 3. Create 2 PENDING Orders
                // ==============================================
                for (int i = 1; i <= 2; i++) {
                        Order pendingOrder = createBaseOrder("ORD-PEND-" + i, branch, customer, OrderStatus.PENDING, now.minusMinutes(10));
                        orderRepository.save(pendingOrder);
                        orderItemRepository.save(createOrderItem(pendingOrder, burger, 1));
                }

                // ==============================================
                // 4. Create 3 PREPARING Orders
                // ==============================================
                for (int i = 1; i <= 3; i++) {
                        Order preparingOrder = createBaseOrder("ORD-PREP-" + i, branch, customer, OrderStatus.PREPARING, now.minusMinutes(20));
                        preparingOrder.setCookingStartedAt(now.minusMinutes(15)); // started cooking 15 mins ago
                        orderRepository.save(preparingOrder);
                        orderItemRepository.save(createOrderItem(preparingOrder, burger, 2));
                        orderItemRepository.save(createOrderItem(preparingOrder, rice, 1));
                }

                // ==============================================
                // 5. Create 4 COMPLETED Orders
                // ==============================================
                createCompletedOrder("ORD-COMP-1", branch, customer, 10, now.minusHours(2), rice, 5); // 5 Rice
                createCompletedOrder("ORD-COMP-2", branch, customer, 12, now.minusHours(3), kottu, 4); // 4 Kottu
                createCompletedOrder("ORD-COMP-3", branch, customer, 18, now.minusHours(4), pasta, 3); // 3 Pasta
                createCompletedOrder("ORD-COMP-4", branch, customer, 20, now.minusHours(5), burger, 2); // 2 Burger
                createCompletedOrder("ORD-COMP-5", branch, customer, 15, now.minusHours(1), pizza, 1); // 1 Pizza

                // ==============================================
                // 6. Seed data for Peak Hours Graph (Approved at various times TODAY)
                // ==============================================
                LocalDate today = LocalDate.now();
                
                // slot: 8AM-10AM (5 orders)
                seedPeakHourOrders("PH-08-", branch, customer, burger, 5, today.atTime(8, 30));
                
                // slot: 10AM-12PM (12 orders)
                seedPeakHourOrders("PH-10-", branch, customer, kottu, 12, today.atTime(10, 45));
                
                // slot: 12PM-2PM (25 orders) - Peak Lunch
                seedPeakHourOrders("PH-12-", branch, customer, rice, 25, today.atTime(12, 15));
                
                // slot: 2PM-4PM (8 orders)
                seedPeakHourOrders("PH-14-", branch, customer, pasta, 8, today.atTime(14, 30));
                
                // slot: 4PM-6PM (15 orders)
                seedPeakHourOrders("PH-16-", branch, customer, pizza, 15, today.atTime(16, 20));
                
                // slot: 6PM-8PM (30 orders) - Peak Dinner
                seedPeakHourOrders("PH-18-", branch, customer, kottu, 30, today.atTime(19, 0));
                
                // ==============================================
                // 7. Seed Inventory Items for Alerts
                // ==============================================
                inventoryItemRepository.deleteAll(); // පරණ දත්ත අයින් කරමු

                // Normal Stock (Show no alert)
                createInventoryItem(branch, "Basmati Rice", 100, 80, 20, "KG");

                // LOW Stock (Quantity 10, Reorder 15 => LOW)
                createInventoryItem(branch, "Maldon Sea Salt", 30, 10, 15, "KG");

                // CRITICAL Stock (Quantity 2, Reorder 6 => CRITICAL since 2 <= 6/2)
                createInventoryItem(branch, "Truffle Oil", 10, 2, 6, "LITERS");

                // CRITICAL Stock (Quantity 4, Reorder 10 => CRITICAL)
                createInventoryItem(branch, "Wagyu Beef (A5)", 20, 4, 10, "KG");

                System.out.println("✅ Data seeding successfully completed!");
                System.out.println("Expected Dashboard Stats in Frontend:");
                System.out.println("- Pending Orders  : 2");
                System.out.println("- Preparing Orders: 3");
                System.out.println("- Completed Orders: 4");
                System.out.println("- Avg Prep Time   : 15.0 Minutes");
                System.out.println("=================================================");
        }

        private Order createBaseOrder(String orderNumber, Branch branch, Customer customer, OrderStatus status, LocalDateTime createdAt) {
                Order order = new Order();
                order.setOrderNumber(orderNumber);
                order.setBranch(branch);
                order.setCustomer(customer);
                order.setOrderType(OrderType.QR);
                order.setStatus(status);
                order.setTotalAmount(BigDecimal.valueOf(25.00));
                order.setDiscountAmount(BigDecimal.ZERO);
                order.setFinalAmount(BigDecimal.valueOf(25.00));
                order.setPaymentStatus(PaymentStatus.PENDING);
                order.setCreatedAt(createdAt);
                return order;
        }

        private void createCompletedOrder(String orderNumber, Branch branch, Customer customer, int prepTimeMinutes, LocalDateTime startAt, MenuItem menuItem, int qty) {
                Order order = createBaseOrder(orderNumber, branch, customer, OrderStatus.COMPLETED, startAt.minusMinutes(5));
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setCookingStartedAt(startAt);
                order.setCookingCompletedAt(startAt.plusMinutes(prepTimeMinutes));

                order = orderRepository.save(order);

                orderItemRepository.save(createOrderItem(order, menuItem, qty));
        }

        private OrderItem createOrderItem(Order order, MenuItem menuItem, int qty) {
                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setMenuItem(menuItem);
                item.setItemName(menuItem.getName());
                item.setUnitPrice(menuItem.getPrice());
                item.setSubtotal(menuItem.getPrice().multiply(BigDecimal.valueOf(qty)));
                item.setQuantity(qty);
                return item;
        }

        private void seedPeakHourOrders(String prefix, Branch branch, Customer customer, MenuItem menuItem, int orderCount, LocalDateTime approvedTime) {
                for (int i = 1; i <= orderCount; i++) {
                        Order order = createBaseOrder(prefix + i, branch, customer, OrderStatus.PENDING, approvedTime.minusMinutes(5));
                        order.setApprovedAt(approvedTime); // Essential for Peak Hours logic
                        orderRepository.save(order);
                        orderItemRepository.save(createOrderItem(order, menuItem, 1));
                }
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
