package com.ByteKnights.com.resturarent_system.config;

import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.entity.BranchStatus;
import com.ByteKnights.com.resturarent_system.entity.MenuCategory;
import com.ByteKnights.com.resturarent_system.entity.MenuItem;
import com.ByteKnights.com.resturarent_system.entity.MenuItemStatus;
import com.ByteKnights.com.resturarent_system.entity.Role;
import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import com.ByteKnights.com.resturarent_system.repository.MenuCategoryRepository;
import com.ByteKnights.com.resturarent_system.repository.MenuItemRepository;
import com.ByteKnights.com.resturarent_system.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

@Component
public class DataSeeder implements CommandLineRunner {

        private static final Long SEED_CREATED_BY = 1L;

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
        public void run(String... args) {
                System.out.println("Seeding data...");

                roleRepository.findByName("ROLE_CUSTOMER").orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name("ROLE_CUSTOMER")
                                .description("Customer Role")
                                .build()
                ));

                Branch branch = branchRepository.findByNameIgnoreCase("Main Branch").orElseGet(() -> branchRepository.save(
                        Branch.builder()
                                .name("Main Branch")
                                .address("123 Food St")
                                .contactNumber("0112345678")
                                .email("main@kitchen.com")
                                .status(BranchStatus.ACTIVE)
                                .build()
                ));

                Map<String, MenuCategory> categoriesByName = Map.of(
                        "Burgers", upsertCategory("Burgers", "Premium handcrafted burgers"),
                        "Pizza", upsertCategory("Pizza", "Stone-baked artisan pizzas"),
                        "Desserts", upsertCategory("Desserts", "Signature desserts"),
                        "Pasta", upsertCategory("Pasta", "Italian pasta collection"),
                        "Salads", upsertCategory("Salads", "Fresh healthy bowls and salads"),
                        "Beverages", upsertCategory("Beverages", "Fresh and crafted drinks")
                );

                upsertMenuItem(branch, categoriesByName.get("Burgers"), "Wagyu Beef Burger", "Signature Burgers",
                        "Premium Japanese wagyu patty, aged cheddar, caramelized onions, truffle aioli",
                        BigDecimal.valueOf(500.00),
                        "https://cdn.prod.website-files.com/65fc1fa2c1e7707c3f051466/69263773f626fe9424210272_750f721e-ad71-4daa-8601-bc3c78b9587d.webp",
                        22);

                upsertMenuItem(branch, categoriesByName.get("Pizza"), "Margherita Napoletana", "Classic Pizza",
                        "San Marzano tomatoes, buffalo mozzarella, fresh basil, extra virgin",
                        BigDecimal.valueOf(1200.00),
                        "https://mediterraneanrecipes.com.au/wp-content/uploads/2024/01/Margherita-Pizza.jpg",
                        18);

                upsertMenuItem(branch, categoriesByName.get("Desserts"), "Molten Chocolate Souffle", "Dessert Specials",
                        "Valrhona dark chocolate, vanilla bean ice cream, gold leaf, raspberry coulis",
                        BigDecimal.valueOf(850.00),
                        "https://karenehman.com/wp-content/uploads/2024/10/Hot-Fudge-Sundae-Cake-Take-two.jpg",
                        25);

                upsertMenuItem(branch, categoriesByName.get("Burgers"), "Signature BBQ Burger", "Signature Burgers",
                        "Double angus beef, applewood bacon, aged cheddar, house BBQ sauce.",
                        BigDecimal.valueOf(950.00),
                        "https://mefamilyfarm.com/cdn/shop/files/mae-mu-I7A_pHLcQK8-unsplash.jpg?v=1751451226&width=1445",
                        22);

                upsertMenuItem(branch, categoriesByName.get("Pizza"), "Tartufo Bianco Pizza", "Gourmet Pizza",
                        "White truffle cream, wild mushrooms, fontina cheese, arugula, white truffle oil",
                        BigDecimal.valueOf(1400.00),
                        "https://images.unsplash.com/photo-1628840042765-356cda07504e?fm=jpg&q=60&w=3000&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8cGVwcGVyb25pJTIwcGl6emF8ZW58MHx8MHx8fDA=",
                        18);

                upsertMenuItem(branch, categoriesByName.get("Pasta"), "Truffle Carbonara", "Classic Pasta",
                        "Fresh pasta, Italian pancetta, organic eggs, aged parmesan",
                        BigDecimal.valueOf(1200.00),
                        "https://www.salepepe.com/media-library/image.jpg?id=26691626&width=1200&height=1200&coordinates=1058,0,1058,0",
                        20);

                upsertMenuItem(branch, categoriesByName.get("Salads"), "Mediterranean Quinoa Bowl", "Healthy Bowls",
                        "Organic quinoa, roasted vegetables, feta cheese, olives, lemon herb",
                        BigDecimal.valueOf(890.00),
                        "https://cafeconnection.org/wp-content/uploads/2021/10/monika-grabkowska-pCxJvSeSB5A-unsplash-edited-scaled.jpg",
                        15);

                upsertMenuItem(branch, categoriesByName.get("Beverages"), "Artisan Lemonade", "Fresh Drinks",
                        "Fresh-squeezed lemons, organic honey, fresh mint, sparkling water",
                        BigDecimal.valueOf(990.00),
                        "https://ellis.be/content/uploads/2021/07/CraftLemonadeLemon_Website.jpg",
                        8);

                System.out.println("Data seeding completed.");
        }

        private MenuCategory upsertCategory(String name, String description) {
                MenuCategory category = menuCategoryRepository.findByName(name)
                        .orElseGet(() -> MenuCategory.builder().name(name).build());

                category.setDescription(description);
                return menuCategoryRepository.save(category);
        }

        private MenuItem upsertMenuItem(Branch branch,
                                        MenuCategory category,
                                        String name,
                                        String subCategory,
                                        String description,
                                        BigDecimal price,
                                        String imageUrl,
                                        Integer preparationTime) {
                MenuItem item = menuItemRepository.findAll().stream()
                        .filter(existing -> existing.getBranch() != null
                                && Objects.equals(existing.getBranch().getId(), branch.getId())
                                && existing.getName() != null
                                && existing.getName().equalsIgnoreCase(name))
                        .findFirst()
                        .orElseGet(() -> MenuItem.builder().branch(branch).name(name).build());

                item.setCategory(category);
                item.setSubCategory(subCategory);
                item.setDescription(description);
                item.setPrice(price);
                item.setImageUrl(imageUrl);
                item.setIsAvailable(true);
                item.setStatus(MenuItemStatus.APPROVED);
                item.setPreparationTime(preparationTime);
                item.setCreatedBy(SEED_CREATED_BY);

                return menuItemRepository.save(item);
        }
}
