package com.ByteKnights.com.resturarent_system.config;

import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
                System.out.println("Seeding data...");

                // Create Role
                Role customerRole = roleRepository.findByName("ROLE_CUSTOMER").orElseGet(() -> {
                        Role role = Role.builder()
                                        .name("ROLE_CUSTOMER")
                                        .description("Customer Role")
                                        .build();
                        return roleRepository.save(role);
                });

                // Create or reuse Branch #1
                Branch branch = branchRepository.findById(1L).orElseGet(() -> branchRepository.save(
                                Branch.builder()
                                                .name("Main Branch")
                                                .address("123 Food St")
                                                .contactNumber("0112345678")
                                                .email("main@kitchen.com")
                                                .status(BranchStatus.ACTIVE)
                                                .build()
                ));

                // Create User for Customer
                User user = userRepository.findByEmail("john@example.com").orElseGet(() -> userRepository.save(
                                User.builder()
                                                .username("john_doe")
                                                .email("john@example.com")
                                                .password("password123") // In real app, encode this
                                                .phone("0771234567")
                                                .role(customerRole)
                                                .build()
                ));

                // Create Customer
                Customer customer = customerRepository.findByUser(user).orElseGet(() -> customerRepository.save(
                                Customer.builder()
                                                .user(user)
                                                .loyaltyPoints(100)
                                                .totalSpent(BigDecimal.ZERO)
                                                .build()
                ));

                // Create / update Categories
                Map<String, MenuCategory> categoriesByName = Map.of(
                                "Burgers", upsertCategory("Burgers", "Gourmet handcrafted burgers"),
                                "Pizza", upsertCategory("Pizza", "Authentic wood-fired pizzas"),
                                "Desserts", upsertCategory("Desserts", "Sweet treats and after-dinner delights"),
                                "Pasta", upsertCategory("Pasta", "Freshly made Italian pasta dishes"),
                                "Salads", upsertCategory("Salads", "Healthy and fresh organic salads"),
                                "Beverages", upsertCategory("Beverages", "Refreshing drinks and handcrafted cocktails")
                );

                // Create / update Menu Items for branch 1 with image URLs and APPROVED status
                MenuItem wagyuBurger = upsertMenuItem(
                                branch,
                                categoriesByName.get("Burgers"),
                                "Wagyu Beef Burger",
                                "Premium Japanese wagyu patty, aged cheddar, caramelized onions, truffle aioli",
                                BigDecimal.valueOf(500.00),
                                "https://cdn.prod.website-files.com/65fc1fa2c1e7707c3f051466/69263773f626fe9424210272_750f721e-ad71-4daa-8601-bc3c78b9587d.webp",
                                22
                );

                MenuItem margheritaPizza = upsertMenuItem(
                                branch,
                                categoriesByName.get("Pizza"),
                                "Margherita Napoletana",
                                "San Marzano tomatoes, buffalo mozzarella, fresh basil, extra virgin",
                                BigDecimal.valueOf(1200.00),
                                "https://mediterraneanrecipes.com.au/wp-content/uploads/2024/01/Margherita-Pizza.jpg",
                                18
                );

                MenuItem moltenSouffle = upsertMenuItem(
                                branch,
                                categoriesByName.get("Desserts"),
                                "Molten Chocolate Souffle",
                                "Valrhona dark chocolate, vanilla bean ice cream, gold leaf, raspberry coulis",
                                BigDecimal.valueOf(850.00),
                                "https://karenehman.com/wp-content/uploads/2024/10/Hot-Fudge-Sundae-Cake-Take-two.jpg",
                                25
                );

                MenuItem bbqBurger = upsertMenuItem(
                                branch,
                                categoriesByName.get("Burgers"),
                                "Signature BBQ Burger",
                                "Double angus beef, applewood bacon, aged cheddar, house BBQ sauce.",
                                BigDecimal.valueOf(950.00),
                                "https://mefamilyfarm.com/cdn/shop/files/mae-mu-I7A_pHLcQK8-unsplash.jpg?v=1751451226&width=1445",
                                22
                );

                MenuItem tartufoPizza = upsertMenuItem(
                                branch,
                                categoriesByName.get("Pizza"),
                                "Tartufo Bianco Pizza",
                                "White truffle cream, wild mushrooms, fontina cheese, arugula, white truffle oil",
                                BigDecimal.valueOf(1400.00),
                                "https://images.unsplash.com/photo-1628840042765-356cda07504e?fm=jpg&q=60&w=3000&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8cGVwcGVyb25pJTIwcGl6emF8ZW58MHx8MHx8fDA=",
                                18
                );

                MenuItem truffleCarbonara = upsertMenuItem(
                                branch,
                                categoriesByName.get("Pasta"),
                                "Truffle Carbonara",
                                "Fresh pasta, Italian pancetta, organic eggs, aged parmesan",
                                BigDecimal.valueOf(1200.00),
                                "https://www.salepepe.com/media-library/image.jpg?id=26691626&width=1200&height=1200&coordinates=1058,0,1058,0",
                                20
                );

                MenuItem quinoaBowl = upsertMenuItem(
                                branch,
                                categoriesByName.get("Salads"),
                                "Mediterranean Quinoa Bowl",
                                "Organic quinoa, roasted vegetables, feta cheese, olives, lemon herb",
                                BigDecimal.valueOf(890.00),
                                "https://cafeconnection.org/wp-content/uploads/2021/10/monika-grabkowska-pCxJvSeSB5A-unsplash-edited-scaled.jpg",
                                15
                );

                MenuItem artisanLemonade = upsertMenuItem(
                                branch,
                                categoriesByName.get("Beverages"),
                                "Artisan Lemonade",
                                "Fresh-squeezed lemons, organic honey, fresh mint, sparkling water",
                                BigDecimal.valueOf(990.00),
                                "https://ellis.be/content/uploads/2021/07/CraftLemonadeLemon_Website.jpg",
                                8
                );

                // Cleanup legacy starter items from previous seed format
                menuItemRepository.deleteByBranchIdAndNameIn(branch.getId(), List.of("Chicken Burger", "French Fries"));

                if (orderRepository.count() == 0) {
                        // Create Order 1 (Pending) - 9:00 AM
                        Order order1 = new Order();
                        order1.setOrderNumber("ORD-1204");
                        order1.setBranch(branch);
                        order1.setCustomer(customer);
                        order1.setOrderType(OrderType.QR);
                        order1.setStatus(OrderStatus.PLACED);
                        order1.setTotalAmount(wagyuBurger.getPrice().add(artisanLemonade.getPrice()));
                        order1.setDiscountAmount(BigDecimal.ZERO);
                        order1.setFinalAmount(wagyuBurger.getPrice().add(artisanLemonade.getPrice()));
                        order1.setPaymentStatus(PaymentStatus.PENDING);
                        order1.setCreatedAt(java.time.LocalDateTime.of(java.time.LocalDate.now(), java.time.LocalTime.of(9, 0)));
                        order1 = orderRepository.save(order1);

                        OrderItem item1 = OrderItem.builder()
                                        .order(order1)
                                        .menuItem(wagyuBurger)
                                        .itemName(wagyuBurger.getName())
                                        .quantity(1)
                                        .unitPrice(wagyuBurger.getPrice())
                                        .subtotal(wagyuBurger.getPrice())
                                        .build();
                        orderItemRepository.save(item1);

                        OrderItem item2 = OrderItem.builder()
                                        .order(order1)
                                        .menuItem(artisanLemonade)
                                        .itemName(artisanLemonade.getName())
                                        .quantity(1)
                                        .unitPrice(artisanLemonade.getPrice())
                                        .subtotal(artisanLemonade.getPrice())
                                        .build();
                        orderItemRepository.save(item2);

                        // Create Order 2 - 9:30 AM
                        Order order2 = new Order();
                        order2.setOrderNumber("ORD-1200");
                        order2.setBranch(branch);
                        order2.setCustomer(customer);
                        order2.setOrderType(OrderType.QR);
                        order2.setStatus(OrderStatus.PLACED);
                        order2.setTotalAmount(wagyuBurger.getPrice().multiply(BigDecimal.valueOf(2)).add(artisanLemonade.getPrice()));
                        order2.setDiscountAmount(BigDecimal.ZERO);
                        order2.setFinalAmount(wagyuBurger.getPrice().multiply(BigDecimal.valueOf(2)).add(artisanLemonade.getPrice()));
                        order2.setPaymentStatus(PaymentStatus.PAID);
                        order2.setCreatedAt(java.time.LocalDateTime.of(java.time.LocalDate.now(), java.time.LocalTime.of(9, 30)));
                        order2 = orderRepository.save(order2);

                        OrderItem item3 = OrderItem.builder()
                                        .order(order2)
                                        .menuItem(wagyuBurger)
                                        .itemName(wagyuBurger.getName())
                                        .quantity(2)
                                        .unitPrice(wagyuBurger.getPrice())
                                        .subtotal(wagyuBurger.getPrice().multiply(BigDecimal.valueOf(2)))
                                        .build();
                        orderItemRepository.save(item3);

                        OrderItem item3_2 = OrderItem.builder()
                                        .order(order2)
                                        .menuItem(artisanLemonade)
                                        .itemName(artisanLemonade.getName())
                                        .quantity(1)
                                        .unitPrice(artisanLemonade.getPrice())
                                        .subtotal(artisanLemonade.getPrice())
                                        .build();
                        orderItemRepository.save(item3_2);

                        // Create Order 3 - 11:00 AM
                        Order order3 = new Order();
                        order3.setOrderNumber("ORD-1205");
                        order3.setBranch(branch);
                        order3.setCustomer(customer);
                        order3.setOrderType(OrderType.ONLINE);
                        order3.setStatus(OrderStatus.PLACED);
                        order3.setTotalAmount(truffleCarbonara.getPrice());
                        order3.setDiscountAmount(BigDecimal.ZERO);
                        order3.setFinalAmount(truffleCarbonara.getPrice());
                        order3.setPaymentStatus(PaymentStatus.PENDING);
                        order3.setCreatedAt(java.time.LocalDateTime.of(java.time.LocalDate.now(), java.time.LocalTime.of(11, 0)));
                        order3 = orderRepository.save(order3);

                        OrderItem item4 = OrderItem.builder()
                                        .order(order3)
                                        .menuItem(truffleCarbonara)
                                        .itemName(truffleCarbonara.getName())
                                        .quantity(1)
                                        .unitPrice(truffleCarbonara.getPrice())
                                        .subtotal(truffleCarbonara.getPrice())
                                        .build();
                        orderItemRepository.save(item4);

                        System.out.println("Seeding completed for Order 1, 2, 3.");
                }

                // touch these references to avoid accidental cleanup by formatters in case they are
                // only used for seeding data updates in this block
                margheritaPizza.getId();
                moltenSouffle.getId();
                bbqBurger.getId();
                tartufoPizza.getId();
                quinoaBowl.getId();

                System.out.println("Data seeding completed.");
        }

        private MenuCategory upsertCategory(String name, String description) {
                MenuCategory category = menuCategoryRepository.findByName(name).orElseGet(() -> MenuCategory.builder().build());
                category.setName(name);
                category.setDescription(description);
                return menuCategoryRepository.save(category);
        }

        private MenuItem upsertMenuItem(Branch branch,
                                        MenuCategory category,
                                        String name,
                                        String description,
                                        BigDecimal price,
                                        String imageUrl,
                                        Integer preparationTime) {
                MenuItem item = menuItemRepository.findByBranchIdAndName(branch.getId(), name)
                                .orElseGet(() -> MenuItem.builder().branch(branch).name(name).build());

                item.setBranch(branch);
                item.setCategory(category);
                item.setName(name);
                item.setDescription(description);
                item.setPrice(price);
                item.setImageUrl(imageUrl);
                item.setIsAvailable(true);
                item.setStatus(MenuItemStatus.APPROVED);
                item.setPreparationTime(preparationTime);
                item.setSubCategory(null);

                return menuItemRepository.save(item);
        }
}
