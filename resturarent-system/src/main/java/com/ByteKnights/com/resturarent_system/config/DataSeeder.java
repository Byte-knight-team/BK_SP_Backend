package com.ByteKnights.com.resturarent_system.config;

import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

        public DataSeeder(BranchRepository branchRepository,
                          CustomerRepository customerRepository,
                          OrderRepository orderRepository,
                          UserRepository userRepository,
                          RoleRepository roleRepository,
                          MenuCategoryRepository menuCategoryRepository,
                          MenuItemRepository menuItemRepository,
                          OrderItemRepository orderItemRepository) {
                this.branchRepository = branchRepository;
                this.customerRepository = customerRepository;
                this.orderRepository = orderRepository;
                this.userRepository = userRepository;
                this.roleRepository = roleRepository;
                this.menuCategoryRepository = menuCategoryRepository;
                this.menuItemRepository = menuItemRepository;
                this.orderItemRepository = orderItemRepository;
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
                        .price(BigDecimal.valueOf(15.99)).isAvailable(true)
                        .status(MenuItemStatus.APPROVED).preparationTime(15).build());

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
                }

                // ==============================================
                // 5. Create 4 COMPLETED Orders
                // prep times: 10, 12, 18, 20 mins => Avg = 15 mins
                // ==============================================
                createCompletedOrder("ORD-COMP-1", branch, customer, 10, now.minusHours(2));
                createCompletedOrder("ORD-COMP-2", branch, customer, 12, now.minusHours(3));
                createCompletedOrder("ORD-COMP-3", branch, customer, 18, now.minusHours(4));
                createCompletedOrder("ORD-COMP-4", branch, customer, 20, now.minusHours(5));

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

        private void createCompletedOrder(String orderNumber, Branch branch, Customer customer, int prepTimeMinutes, LocalDateTime startAt) {
                Order order = createBaseOrder(orderNumber, branch, customer, OrderStatus.COMPLETED, startAt.minusMinutes(5));
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setCookingStartedAt(startAt);
                order.setCookingCompletedAt(startAt.plusMinutes(prepTimeMinutes));

                order = orderRepository.save(order);

                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setItemName("Chicken Burger");
                item.setQuantity(1);
                item.setUnitPrice(BigDecimal.valueOf(15.99));
                item.setSubtotal(BigDecimal.valueOf(15.99));

                orderItemRepository.save(item);
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
}
