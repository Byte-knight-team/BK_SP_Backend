package com.ByteKnights.com.resturarent_system.config;

import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds initial data on application startup if the DB is empty.
 * Creates a default branch + menu items matching the frontend's hardcoded menu.
 * Safe to re-run: checks if data already exists before inserting.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final BranchRepository branchRepository;
    private final MenuItemRepository menuItemRepository;
    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        seedRoles();
        Branch branch = seedBranch();
        seedMenuItems(branch);
    }

    private void seedRoles() {
        if (roleRepository.count() == 0) {
            roleRepository.save(Role.builder().name("CUSTOMER").description("Customer role").build());
            roleRepository.save(Role.builder().name("STAFF").description("Staff role").build());
            roleRepository.save(Role.builder().name("MANAGER").description("Manager role").build());
            System.out.println("✔ Seeded roles: CUSTOMER, STAFF, MANAGER");
        }
    }

    private Branch seedBranch() {
        if (branchRepository.count() == 0) {
            Branch branch = branchRepository.save(Branch.builder()
                    .name("Crave House - Main Branch")
                    .address("123 Galle Road, Colombo 03")
                    .contactNumber("0112345678")
                    .email("main@cravehouse.lk")
                    .status(BranchStatus.ACTIVE)
                    .build());
            System.out.println("✔ Seeded main branch (id=" + branch.getId() + ")");
            return branch;
        }
        return branchRepository.findAll().get(0);
    }

    private void seedMenuItems(Branch branch) {
        if (menuItemRepository.count() == 0) {
            menuItemRepository.save(menuItem(branch, "Burgers", "Wagyu Beef Burger",
                    "Premium Japanese wagyu patty, aged cheddar, caramelized onions, truffle aioli",
                    500, "https://cdn.prod.website-files.com/65fc1fa2c1e7707c3f051466/69263773f626fe9424210272_750f721e-ad71-4daa-8601-bc3c78b9587d.webp", 22));

            menuItemRepository.save(menuItem(branch, "Pizza", "Margherita Napoletana",
                    "San Marzano tomatoes, buffalo mozzarella, fresh basil, extra virgin",
                    1200, "https://mediterraneanrecipes.com.au/wp-content/uploads/2024/01/Margherita-Pizza.jpg", 18));

            menuItemRepository.save(menuItem(branch, "Desserts", "Molten Chocolate Soufflé",
                    "Valrhona dark chocolate, vanilla bean ice cream, gold leaf, raspberry coulis",
                    850, "https://karenehman.com/wp-content/uploads/2024/10/Hot-Fudge-Sundae-Cake-Take-two.jpg", 25));

            menuItemRepository.save(menuItem(branch, "Burgers", "Signature BBQ Burger",
                    "Double angus beef, applewood bacon, aged cheddar, house BBQ sauce",
                    950, "https://mefamilyfarm.com/cdn/shop/files/mae-mu-I7A_pHLcQK8-unsplash.jpg?v=1751451226&width=1445", 22));

            menuItemRepository.save(menuItem(branch, "Pizza", "Tartufo Bianco Pizza",
                    "White truffle cream, wild mushrooms, fontina cheese, arugula, white truffle oil",
                    1400, "https://images.unsplash.com/photo-1628840042765-356cda07504e", 18));

            menuItemRepository.save(menuItem(branch, "Pasta", "Truffle Carbonara",
                    "Fresh pasta, Italian pancetta, organic eggs, aged parmesan",
                    1200, "https://www.salepepe.com/media-library/image.jpg?id=26691626&width=1200&height=1200", 20));

            menuItemRepository.save(menuItem(branch, "Salads", "Mediterranean Quinoa Bowl",
                    "Organic quinoa, roasted vegetables, feta cheese, olives, lemon herb",
                    890, "https://cafeconnection.org/wp-content/uploads/2021/10/monika-grabkowska-pCxJvSeSB5A-unsplash-edited-scaled.jpg", 15));

            menuItemRepository.save(menuItem(branch, "Beverages", "Artisan Lemonade",
                    "Fresh-squeezed lemons, organic honey, fresh mint, sparkling water",
                    990, "https://ellis.be/content/uploads/2021/07/CraftLemonadeLemon_Website.jpg", 8));

            System.out.println("✔ Seeded 8 menu items");
        }
    }

    private MenuItem menuItem(Branch branch, String category, String name, String desc,
                              int price, String imageUrl, int prepTime) {
        return MenuItem.builder()
                .branch(branch)
                .category(category)
                .name(name)
                .description(desc)
                .price(BigDecimal.valueOf(price))
                .imageUrl(imageUrl)
                .isAvailable(true)
                .preparationTime(prepTime)
                .build();
    }
}
