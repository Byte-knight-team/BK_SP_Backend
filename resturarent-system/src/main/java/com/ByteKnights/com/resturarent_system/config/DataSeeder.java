package com.ByteKnights.com.resturarent_system.config;

import com.ByteKnights.com.resturarent_system.entity.Privilege;
import com.ByteKnights.com.resturarent_system.entity.Role;
import com.ByteKnights.com.resturarent_system.repository.PrivilegeRepository;
import com.ByteKnights.com.resturarent_system.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

// ===== KITCHEN TEST DATA IMPORTS (START) =====
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
// ===== KITCHEN TEST DATA IMPORTS (END) =====

@Component
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;

    // ===== KITCHEN TEST DATA REPOSITORIES (START) =====
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    // ===== KITCHEN TEST DATA REPOSITORIES (END) =====

    public DataSeeder(RoleRepository roleRepository, PrivilegeRepository privilegeRepository,
                      // ===== KITCHEN TEST DATA CONSTRUCTOR PARAMS (START) =====
                      BranchRepository branchRepository, UserRepository userRepository,
                      CustomerRepository customerRepository, MenuCategoryRepository menuCategoryRepository,
                      MenuItemRepository menuItemRepository, InventoryItemRepository inventoryItemRepository,
                      OrderRepository orderRepository, PasswordEncoder passwordEncoder
                      // ===== KITCHEN TEST DATA CONSTRUCTOR PARAMS (END) =====
    ) {
        this.roleRepository = roleRepository;
        this.privilegeRepository = privilegeRepository;

        // ===== KITCHEN TEST DATA ASSIGNMENTS (START) =====
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.menuCategoryRepository = menuCategoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder;
        // ===== KITCHEN TEST DATA ASSIGNMENTS (END) =====
    }

    @Override
    @Transactional
    public void run(String... args) {

        //Creating the privileges
        Privilege createStaff = createPrivilege("CREATE_STAFF");
        Privilege assignPrivileges = createPrivilege("ASSIGN_PRIVILEGES");
        Privilege updateConfig = createPrivilege("UPDATE_CONFIG");
        Privilege viewAudit = createPrivilege("VIEW_AUDIT_LOG");
        Privilege manageBranch = createPrivilege("MANAGE_BRANCH");
        Privilege manageSystemConfig = createPrivilege("MANAGE_SYSTEM_CONFIG");
        Privilege manageOrders = createPrivilege("MANAGE_ORDERS");
        Privilege manageMenu = createPrivilege("MANAGE_MENU");
        Privilege manageReservations = createPrivilege("MANAGE_RESERVATIONS");
        Privilege updateOrderStatus = createPrivilege("UPDATE_ORDER_STATUS");
        Privilege updateDeliveryStatus = createPrivilege("UPDATE_DELIVERY_STATUS");
        Privilege createOrders = createPrivilege("CREATE_ORDERS");
        Privilege viewBranch = createPrivilege("VIEW_BRANCH");
        Privilege viewCustomer = createPrivilege("VIEW_CUSTOMER");
        Privilege viewOwnOrders = createPrivilege("VIEW_OWN_ORDERS");
        Privilege viewOwnProfile = createPrivilege("VIEW_OWN_PROFILE");
        Privilege viewReports = createPrivilege("VIEW_REPORTS");
        Privilege viewOrders = createPrivilege("VIEW_ORDERS");
        Privilege viewDelivery = createPrivilege("VIEW_DELIVERY");

        //Creating the roles
        Role superAdminRole = createRole("SUPER_ADMIN");
        //Adding the privileges to the super admin role
        superAdminRole.setPermissions(new HashSet<>(Set.of(
                createStaff, assignPrivileges, updateConfig, viewAudit, manageBranch, manageSystemConfig,
                manageOrders, manageMenu, manageReservations, updateOrderStatus, updateDeliveryStatus,
                createOrders, viewBranch, viewCustomer, viewOwnOrders, viewOwnProfile, viewReports,
                viewOrders, viewDelivery
        )));
        roleRepository.save(superAdminRole);

        Role adminRole = createRole("ADMIN");
        adminRole.setPermissions(new HashSet<>(Set.of(
                createStaff, assignPrivileges, updateConfig, viewAudit, manageBranch
        )));
        roleRepository.save(adminRole);

        Role managerRole = createRole("MANAGER");
        managerRole.setPermissions(new HashSet<>(Set.of(
                viewBranch, manageOrders, viewReports, viewCustomer
        )));
        roleRepository.save(managerRole);

        Role chefRole = createRole("CHEF");
        //Adding the privileges to the chef role
        chefRole.setPermissions(new HashSet<>(Set.of(
                manageMenu, updateOrderStatus, viewOrders
        )));
        roleRepository.save(chefRole);

        Role receptionistRole = createRole("RECEPTIONIST");
        receptionistRole.setPermissions(new HashSet<>(Set.of(
                createOrders, manageReservations, viewCustomer
        )));
        roleRepository.save(receptionistRole);

        Role deliveryRole = createRole("DELIVERY");
        deliveryRole.setPermissions(new HashSet<>(Set.of(
                updateDeliveryStatus, viewDelivery
        )));
        roleRepository.save(deliveryRole);

        Role customerRole = createRole("CUSTOMER");
        customerRole.setPermissions(new HashSet<>(Set.of(
                viewOwnOrders, viewOwnProfile
        )));
        roleRepository.save(customerRole);

        // =====================================================================
        // ===== KITCHEN MODULE TEST DATA (START) ==============================
        // ===== Purpose: Seed test data for Kitchen API testing             ===
        // ===== APIs tested: /stats, /popular-meals, /peak-hours,           ===
        // =====              /inventory-alerts, /orders, /inventory/all      ===
        // ===== Safe to remove: YES - only test data, does not affect RBAC  ===
        // =====================================================================
        seedKitchenTestData(customerRole);
        // =====================================================================
        // ===== KITCHEN MODULE TEST DATA (END) ================================
        // =====================================================================
    }

    private Privilege createPrivilege(String name) {
        return privilegeRepository.findByName(name).orElseGet(() -> {
            Privilege privilege = Privilege.builder().name(name).build();
            return privilegeRepository.save(privilege);
        });
    }

    private Role createRole(String name) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = Role.builder().name(name).build();
            return roleRepository.save(role);
        });
    }

    // =====================================================================
    // ===== KITCHEN TEST DATA METHOD (START) ==============================
    // ===== You can edit inventory items, orders, menu items below.      ===
    // ===== Guard: only runs if inventory_items table is empty.          ===
    // =====================================================================
    private void seedKitchenTestData(Role customerRole) {
        // Guard: skip if test data already exists
        if (inventoryItemRepository.count() > 0) {
            return;
        }

        // --- 1. Create a test branch ---
        Branch testBranch = branchRepository.findByNameIgnoreCase("Crave House - Colombo")
                .orElseGet(() -> branchRepository.save(Branch.builder()
                        .name("Crave House - Colombo")
                        .address("123 Galle Road, Colombo 03")
                        .contactNumber("0112345678")
                        .email("colombo@cravehouse.lk")
                        .build()));

        // --- 2. Create a test customer (needed for orders) ---
        User testCustomerUser = userRepository.findByUsername("testcustomer01").orElseGet(() -> {
            User user = User.builder()
                    .fullName("Test Customer")
                    .username("testcustomer01")
                    .email("testcustomer01@test.com")
                    .phone("0771234567")
                    .password(passwordEncoder.encode("Pwd@123"))
                    .role(customerRole)
                    .isActive(true)
                    .passwordChanged(true)
                    .inviteStatus(InviteStatus.SENT)
                    .emailSent(true)
                    .build();
            return userRepository.save(user);
        });

        Customer testCustomer = customerRepository.findByUser(testCustomerUser)
                .orElseGet(() -> customerRepository.save(Customer.builder()
                        .user(testCustomerUser)
                        .loyaltyPoints(0)
                        .totalSpent(BigDecimal.ZERO)
                        .emailVerified(false)
                        .phoneVerified(false)
                        .build()));

        // --- 3. Create menu categories and items ---
        MenuCategory mainCourse = menuCategoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Main Course")).findFirst()
                .orElseGet(() -> menuCategoryRepository.save(MenuCategory.builder()
                        .name("Main Course").description("Main dishes").build()));

        MenuCategory appetizer = menuCategoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Appetizers")).findFirst()
                .orElseGet(() -> menuCategoryRepository.save(MenuCategory.builder()
                        .name("Appetizers").description("Starters").build()));

        MenuItem chickenRice = createMenuItem("Chicken Fried Rice", mainCourse, testBranch, new BigDecimal("1200.00"), testCustomerUser.getId());
        MenuItem biryani = createMenuItem("Chicken Biryani", mainCourse, testBranch, new BigDecimal("1500.00"), testCustomerUser.getId());
        MenuItem pasta = createMenuItem("Creamy Pasta", mainCourse, testBranch, new BigDecimal("1100.00"), testCustomerUser.getId());
        MenuItem burger = createMenuItem("Cheese Burger", mainCourse, testBranch, new BigDecimal("950.00"), testCustomerUser.getId());
        MenuItem soup = createMenuItem("Tomato Soup", appetizer, testBranch, new BigDecimal("650.00"), testCustomerUser.getId());
        MenuItem springRolls = createMenuItem("Spring Rolls", appetizer, testBranch, new BigDecimal("750.00"), testCustomerUser.getId());

        // --- 4. Create inventory items ---
        createInventoryItem("Garlic", "kg", 10, 50, 100, testBranch);
        createInventoryItem("Tomato", "kg", 15, 60, 120, testBranch);
        createInventoryItem("Onion", "kg", 20, 80, 160, testBranch);
        createInventoryItem("Chicken Breast", "kg", 85, 40, 100, testBranch);
        createInventoryItem("Olive Oil", "Litre", 120, 50, 150, testBranch);
        createInventoryItem("Basmati Rice", "kg", 5, 30, 100, testBranch);
        createInventoryItem("Salt", "kg", 45, 10, 50, testBranch);
        createInventoryItem("Cheese", "kg", 8, 20, 40, testBranch);
        createInventoryItem("Pasta Noodles", "kg", 25, 15, 50, testBranch);
        createInventoryItem("Butter", "kg", 3, 10, 20, testBranch);

        // --- 5. Create orders with different statuses (for dashboard stats, orders page, peak hours) ---
        // PENDING orders
        createTestOrder("ORD-1001", OrderStatus.PENDING, testBranch, testCustomer,
                chickenRice, 2, biryani, 1, LocalDateTime.now().minusHours(1));
        createTestOrder("ORD-1002", OrderStatus.PENDING, testBranch, testCustomer,
                pasta, 1, soup, 2, LocalDateTime.now().minusMinutes(30));
        createTestOrder("ORD-1003", OrderStatus.PENDING, testBranch, testCustomer,
                burger, 3, springRolls, 1, LocalDateTime.now().minusMinutes(15));

        // PREPARING orders
        createTestOrder("ORD-1004", OrderStatus.PREPARING, testBranch, testCustomer,
                chickenRice, 1, biryani, 2, LocalDateTime.now().minusHours(2));
        createTestOrder("ORD-1005", OrderStatus.PREPARING, testBranch, testCustomer,
                pasta, 2, burger, 1, LocalDateTime.now().minusHours(1).minusMinutes(30));

        // COMPLETED orders (with cooking times for avg prep time calculation)
        createCompletedOrder("ORD-1006", testBranch, testCustomer,
                chickenRice, 3, soup, 2,
                LocalDateTime.now().minusDays(1).withHour(12).withMinute(0),
                LocalDateTime.now().minusDays(1).withHour(12).withMinute(25));
        createCompletedOrder("ORD-1007", testBranch, testCustomer,
                biryani, 1, springRolls, 3,
                LocalDateTime.now().minusDays(1).withHour(18).withMinute(0),
                LocalDateTime.now().minusDays(1).withHour(18).withMinute(35));
        createCompletedOrder("ORD-1008", testBranch, testCustomer,
                burger, 2, pasta, 1,
                LocalDateTime.now().minusDays(2).withHour(14).withMinute(0),
                LocalDateTime.now().minusDays(2).withHour(14).withMinute(20));
        createCompletedOrder("ORD-1009", testBranch, testCustomer,
                chickenRice, 4, biryani, 2,
                LocalDateTime.now().minusDays(3).withHour(19).withMinute(0),
                LocalDateTime.now().minusDays(3).withHour(19).withMinute(40));
    }

    // --- Kitchen test data helper methods ---

    private MenuItem createMenuItem(String name, MenuCategory category, Branch branch, BigDecimal price, Long createdById) {
        return menuItemRepository.findAll().stream()
                .filter(m -> m.getName().equals(name) && m.getBranch().getId().equals(branch.getId()))
                .findFirst()
                .orElseGet(() -> menuItemRepository.save(MenuItem.builder()
                        .name(name)
                        .category(category)
                        .branch(branch)
                        .price(price)
                        .isAvailable(true)
                        .status(MenuItemStatus.APPROVED)
                        .createdBy(createdById)
                        .build()));
    }

    private void createInventoryItem(String name, String unit, double qty, double reorder, double max, Branch branch) {
        inventoryItemRepository.save(InventoryItem.builder()
                .name(name)
                .unit(unit)
                .quantity(BigDecimal.valueOf(qty))
                .reorderLevel(BigDecimal.valueOf(reorder))
                .maxStock(BigDecimal.valueOf(max))
                .branch(branch)
                .build());
    }

    private void createTestOrder(String orderNumber, OrderStatus status, Branch branch, Customer customer,
                                 MenuItem item1, int qty1, MenuItem item2, int qty2, LocalDateTime createdTime) {
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setStatus(status);
        order.setOrderType(OrderType.QR);
        order.setBranch(branch);
        order.setCustomer(customer);
        order.setTotalAmount(item1.getPrice().multiply(BigDecimal.valueOf(qty1))
                .add(item2.getPrice().multiply(BigDecimal.valueOf(qty2))));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFinalAmount(order.getTotalAmount());
        order.setCreatedAt(createdTime);
        order.setUpdatedAt(createdTime);
        order.setStatusUpdatedAt(createdTime);
        if (status == OrderStatus.PREPARING) {
            order.setCookingStartedAt(createdTime);
        }

        OrderItem oi1 = OrderItem.builder()
                .itemName(item1.getName()).menuItem(item1).quantity(qty1)
                .unitPrice(item1.getPrice())
                .subtotal(item1.getPrice().multiply(BigDecimal.valueOf(qty1)))
                .status(status == OrderStatus.PENDING ? OrderItemStatus.PENDING : OrderItemStatus.PREPARING)
                .build();

        OrderItem oi2 = OrderItem.builder()
                .itemName(item2.getName()).menuItem(item2).quantity(qty2)
                .unitPrice(item2.getPrice())
                .subtotal(item2.getPrice().multiply(BigDecimal.valueOf(qty2)))
                .status(status == OrderStatus.PENDING ? OrderItemStatus.PENDING : OrderItemStatus.PREPARING)
                .build();

        order.addItem(oi1);
        order.addItem(oi2);
        order.setPaymentStatus(PaymentStatus.PENDING);
        orderRepository.save(order);
    }

    private void createCompletedOrder(String orderNumber, Branch branch, Customer customer,
                                      MenuItem item1, int qty1, MenuItem item2, int qty2,
                                      LocalDateTime startTime, LocalDateTime endTime) {
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setStatus(OrderStatus.COMPLETED);
        order.setOrderType(OrderType.QR);
        order.setBranch(branch);
        order.setCustomer(customer);
        order.setTotalAmount(item1.getPrice().multiply(BigDecimal.valueOf(qty1))
                .add(item2.getPrice().multiply(BigDecimal.valueOf(qty2))));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFinalAmount(order.getTotalAmount());
        order.setCreatedAt(startTime);
        order.setUpdatedAt(endTime);
        order.setStatusUpdatedAt(endTime);
        order.setCookingStartedAt(startTime);
        order.setCookingCompletedAt(endTime);

        OrderItem oi1 = OrderItem.builder()
                .itemName(item1.getName()).menuItem(item1).quantity(qty1)
                .unitPrice(item1.getPrice())
                .subtotal(item1.getPrice().multiply(BigDecimal.valueOf(qty1)))
                .status(OrderItemStatus.READY)
                .build();

        OrderItem oi2 = OrderItem.builder()
                .itemName(item2.getName()).menuItem(item2).quantity(qty2)
                .unitPrice(item2.getPrice())
                .subtotal(item2.getPrice().multiply(BigDecimal.valueOf(qty2)))
                .status(OrderItemStatus.READY)
                .build();

        order.addItem(oi1);
        order.addItem(oi2);
        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);
    }
    // =====================================================================
    // ===== KITCHEN TEST DATA METHOD (END) ================================
    // =====================================================================
}