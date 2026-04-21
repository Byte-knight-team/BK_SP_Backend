package com.ByteKnights.com.resturarent_system.config;

import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class DataSeeder implements CommandLineRunner {

        private final BranchRepository branchRepository;
        private final RoleRepository roleRepository;
        private final MenuCategoryRepository menuCategoryRepository;
        private final MenuItemRepository menuItemRepository;

        public DataSeeder(BranchRepository branchRepository,
                        RoleRepository roleRepository,
                        MenuCategoryRepository menuCategoryRepository,
                        MenuItemRepository menuItemRepository) {
                this.branchRepository = branchRepository;
                this.roleRepository = roleRepository;
                this.menuCategoryRepository = menuCategoryRepository;
                this.menuItemRepository = menuItemRepository;
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

                // Create / update Categories
                Map<String, MenuCategory> categoriesByName = Map.of(
                                "Burgers", upsertCategory("Burgers", "Gourmet handcrafted burgers"),
                                "Pizza", upsertCategory("Pizza", "Authentic wood-fired pizzas"),
                                "Desserts", upsertCategory("Desserts", "Sweet treats and after-dinner delights"),
                                "Pasta", upsertCategory("Pasta", "Freshly made Italian pasta dishes"),
                                "Salads", upsertCategory("Salads", "Healthy and fresh organic salads"),
                                "Beverages", upsertCategory("Beverages", "Refreshing drinks and handcrafted cocktails")
                );

                // Create / update Menu Items for branch 1 with image URLs, APPROVED status, and sub categories
                MenuItem wagyuBurger = upsertMenuItem(
                                branch,
                                categoriesByName.get("Burgers"),
                                "Wagyu Beef Burger",
                                "Premium Japanese wagyu patty, aged cheddar, caramelized onions, truffle aioli",
                                BigDecimal.valueOf(500.00),
                                "https://cdn.prod.website-files.com/65fc1fa2c1e7707c3f051466/69263773f626fe9424210272_750f721e-ad71-4daa-8601-bc3c78b9587d.webp",
                                "Signature Burgers",
                                22
                );

                MenuItem margheritaPizza = upsertMenuItem(
                                branch,
                                categoriesByName.get("Pizza"),
                                "Margherita Napoletana",
                                "San Marzano tomatoes, buffalo mozzarella, fresh basil, extra virgin",
                                BigDecimal.valueOf(1200.00),
                                "https://mediterraneanrecipes.com.au/wp-content/uploads/2024/01/Margherita-Pizza.jpg",
                                "Classic Pizza",
                                18
                );

                MenuItem moltenSouffle = upsertMenuItem(
                                branch,
                                categoriesByName.get("Desserts"),
                                "Molten Chocolate Souffle",
                                "Valrhona dark chocolate, vanilla bean ice cream, gold leaf, raspberry coulis",
                                BigDecimal.valueOf(850.00),
                                "https://karenehman.com/wp-content/uploads/2024/10/Hot-Fudge-Sundae-Cake-Take-two.jpg",
                                "Dessert Specials",
                                25
                );

                MenuItem bbqBurger = upsertMenuItem(
                                branch,
                                categoriesByName.get("Burgers"),
                                "Signature BBQ Burger",
                                "Double angus beef, applewood bacon, aged cheddar, house BBQ sauce.",
                                BigDecimal.valueOf(950.00),
                                "https://mefamilyfarm.com/cdn/shop/files/mae-mu-I7A_pHLcQK8-unsplash.jpg?v=1751451226&width=1445",
                                "Signature Burgers",
                                22
                );

                MenuItem tartufoPizza = upsertMenuItem(
                                branch,
                                categoriesByName.get("Pizza"),
                                "Tartufo Bianco Pizza",
                                "White truffle cream, wild mushrooms, fontina cheese, arugula, white truffle oil",
                                BigDecimal.valueOf(1400.00),
                                "https://images.unsplash.com/photo-1628840042765-356cda07504e?fm=jpg&q=60&w=3000&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8cGVwcGVyb25pJTIwcGl6emF8ZW58MHx8MHx8fDA=",
                                "Gourmet Pizza",
                                18
                );

                MenuItem truffleCarbonara = upsertMenuItem(
                                branch,
                                categoriesByName.get("Pasta"),
                                "Truffle Carbonara",
                                "Fresh pasta, Italian pancetta, organic eggs, aged parmesan",
                                BigDecimal.valueOf(1200.00),
                                "https://www.salepepe.com/media-library/image.jpg?id=26691626&width=1200&height=1200&coordinates=1058,0,1058,0",
                                "Classic Pasta",
                                20
                );

                MenuItem quinoaBowl = upsertMenuItem(
                                branch,
                                categoriesByName.get("Salads"),
                                "Mediterranean Quinoa Bowl",
                                "Organic quinoa, roasted vegetables, feta cheese, olives, lemon herb",
                                BigDecimal.valueOf(890.00),
                                "https://cafeconnection.org/wp-content/uploads/2021/10/monika-grabkowska-pCxJvSeSB5A-unsplash-edited-scaled.jpg",
                                "Healthy Bowls",
                                15
                );

                MenuItem artisanLemonade = upsertMenuItem(
                                branch,
                                categoriesByName.get("Beverages"),
                                "Artisan Lemonade",
                                "Fresh-squeezed lemons, organic honey, fresh mint, sparkling water",
                                BigDecimal.valueOf(990.00),
                                "https://ellis.be/content/uploads/2021/07/CraftLemonadeLemon_Website.jpg",
                                "Fresh Drinks",
                                8
                );

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
                                        String subCategory,
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
                item.setSubCategory(subCategory);

                return menuItemRepository.save(item);
        }
}
