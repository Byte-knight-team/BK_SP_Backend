package com.ByteKnights.com.resturarent_system.config;

import com.ByteKnights.com.resturarent_system.entity.Privilege;
import com.ByteKnights.com.resturarent_system.entity.Role;

import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import com.ByteKnights.com.resturarent_system.repository.ChefRequestRepository;
import com.ByteKnights.com.resturarent_system.repository.CustomerRepository;
import com.ByteKnights.com.resturarent_system.repository.InventoryItemRepository;
import com.ByteKnights.com.resturarent_system.repository.MenuCategoryRepository;
import com.ByteKnights.com.resturarent_system.repository.MenuItemRepository;
import com.ByteKnights.com.resturarent_system.repository.OrderItemRepository;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.repository.PrivilegeRepository;
import com.ByteKnights.com.resturarent_system.repository.RoleRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataSeeder implements CommandLineRunner {

        private final RoleRepository roleRepository;
        private final PrivilegeRepository privilegeRepository;

        public DataSeeder(BranchRepository branchRepository,
                        CustomerRepository customerRepository,
                        OrderRepository orderRepository,
                        UserRepository userRepository,
                        RoleRepository roleRepository,
                        MenuCategoryRepository menuCategoryRepository,
                        MenuItemRepository menuItemRepository,
                        OrderItemRepository orderItemRepository,
                        InventoryItemRepository inventoryItemRepository,
                        ChefRequestRepository chefRequestRepository,
                        PrivilegeRepository privilegeRepository) {

                this.roleRepository = roleRepository;
                this.privilegeRepository = privilegeRepository;
        }

        @Override
        @Transactional
        public void run(String... args) throws Exception {

                /*
                 * Privileges are system-level permission names.
                 *
                 * These can be used in controllers/services like:
                 * 
                 * @PreAuthorize("hasAuthority('CREATE_STAFF')")
                 *
                 * Important:
                 * Use only uppercase snake_case privilege names.
                 * Do not use duplicate camelCase names like createTable or createQRCode.
                 */

                // Staff & RBAC
                Privilege createStaff = createPrivilege("CREATE_STAFF");
                Privilege assignPrivileges = createPrivilege("ASSIGN_PRIVILEGES");

                // System governance
                Privilege updateConfig = createPrivilege("UPDATE_CONFIG");
                Privilege viewAudit = createPrivilege("VIEW_AUDIT_LOG");
                Privilege manageBranch = createPrivilege("MANAGE_BRANCH");
                Privilege manageSystemConfig = createPrivilege("MANAGE_SYSTEM_CONFIG");

                // Orders
                Privilege manageOrders = createPrivilege("MANAGE_ORDERS");
                Privilege updateOrderStatus = createPrivilege("UPDATE_ORDER_STATUS");
                Privilege createOrders = createPrivilege("CREATE_ORDERS");
                Privilege viewOrders = createPrivilege("VIEW_ORDERS");
                Privilege viewOwnOrders = createPrivilege("VIEW_OWN_ORDERS");

                // Menu / reservations / customer
                Privilege manageMenu = createPrivilege("MANAGE_MENU");
                Privilege manageReservations = createPrivilege("MANAGE_RESERVATIONS");
                Privilege viewCustomer = createPrivilege("VIEW_CUSTOMER");
                Privilege viewCatergories = createPrivilege("VIEW_CATEGORIES");
                Privilege viewCategoryById = createPrivilege("VIEW_CATEGORY_BY_ID");
                Privilege createCategory = createPrivilege("CREATE_CATEGORY");
                Privilege updateCategory = createPrivilege("UPDATE_CATEGORY");
                Privilege deleteCategory = createPrivilege("DELETE_CATEGORY");
                Privilege viewPendingItems = createPrivilege("VIEW_PENDING_ITEMS");
                Privilege viewCategoryCount = createPrivilege("VIEW_CATEGORY_COUNT");
                Privilege viewSubCategory = createPrivilege("VIEW_SUBCATEGORY_COUNT");
                Privilege viewItemsCount = createPrivilege("VIEW_ITEMS_COUNT");
                Privilege viewAvailbleItemsCount = createPrivilege("VIEW_AVAILABLE_ITEMS_COUNT");
                Privilege viewAllItems = createPrivilege("VIEW_ALL_ITEMS");
                Privilege viewItemById = createPrivilege("VIEW_ITEM_BY_ID");
                Privilege createItem = createPrivilege("CREATE_ITEM");
                Privilege updateItem = createPrivilege("UPDATE_ITEM");
                Privilege rejectPendingItem = createPrivilege("REJECT_PENDING_ITEM");
                Privilege approvePendingItem = createPrivilege("APPROVE_PENDING_ITEM");
                Privilege toggleItemAvailability = createPrivilege("TOGGLE_ITEM_AVAILABILITY");
                Privilege deleteItem = createPrivilege("DELETE_ITEM");
                Privilege viewAllSubcategories = createPrivilege("VIEW_ALL_SUBCATEGORIES");


                // Delivery
                Privilege updateDeliveryStatus = createPrivilege("UPDATE_DELIVERY_STATUS");
                Privilege viewDelivery = createPrivilege("VIEW_DELIVERY");

                // Branch / reports / profile
                Privilege viewBranch = createPrivilege("VIEW_BRANCH");
                Privilege viewReports = createPrivilege("VIEW_REPORTS");
                Privilege viewOwnProfile = createPrivilege("VIEW_OWN_PROFILE");

                // QR & restaurant table permissions
                Privilege createQrcode = createPrivilege("CREATE_QRCODE");
                Privilege regenerateQrcode = createPrivilege("REGENERATE_QRCODE");
                Privilege revokeQrcode = createPrivilege("REVOKE_QRCODE");

                Privilege createRestaurantTable = createPrivilege("CREATE_RESTAURANT_TABLE");
                Privilege viewRestaurantTable = createPrivilege("VIEW_RESTAURANT_TABLE");
                Privilege viewRestaurantTableById = createPrivilege("VIEW_RESTAURANT_TABLE_BY_ID");
                Privilege updateRestaurantTable = createPrivilege("UPDATE_RESTAURANT_TABLE");
                Privilege deleteRestaurantTable = createPrivilege("DELETE_RESTAURANT_TABLE");

                /*
                 * All known system privileges.
                 *
                 * SUPER_ADMIN will always receive missing privileges from this set.
                 * Other roles will only receive default permissions when they are first
                 * created.
                 */
                Set<Privilege> allPrivileges = Set.of(
                                createStaff,
                                assignPrivileges,
                                updateConfig,
                                viewAudit,
                                manageBranch,
                                manageSystemConfig,
                                manageOrders,
                                updateOrderStatus,
                                createOrders,
                                viewOrders,
                                viewOwnOrders,
                                manageMenu,
                                manageReservations,
                                viewCustomer,
                                updateDeliveryStatus,
                                viewDelivery,
                                viewBranch,
                                viewReports,
                                viewOwnProfile,
                                createQrcode,
                                regenerateQrcode,
                                revokeQrcode,
                                createRestaurantTable,
                                viewRestaurantTable,
                                viewRestaurantTableById,
                                updateRestaurantTable,
                                deleteRestaurantTable,
                                viewCatergories,
                                viewCategoryById,
                                createCategory,
                                updateCategory,
                                deleteCategory,
                                viewPendingItems, 
                                viewCategoryCount,            
                                viewSubCategory,
                                viewItemsCount,
                                viewAvailbleItemsCount, 
                                viewAllItems,
                                viewItemById,
                                createItem,
                                updateItem, 
                                rejectPendingItem, 
                                approvePendingItem,
                                toggleItemAvailability, 
                                deleteItem, 
                                viewAllSubcategories
                        );

                /*
                 * For normal roles, default permissions are added ONLY when the role is first
                 * created.
                 *
                 * This prevents the DataSeeder from overwriting changes made from
                 * the Roles & Permissions page.
                 *
                 * Example:
                 * If SUPER_ADMIN removes CREATE_STAFF from ADMIN using the frontend,
                 * restarting the backend should NOT automatically add it back.
                 */
                Role superAdminRole = createRoleWithDefaultPermissions("SUPER_ADMIN", allPrivileges);

                /*
                 * SUPER_ADMIN is the owner role.
                 *
                 * SUPER_ADMIN always gets all currently known system privileges.
                 * This is safe because SUPER_ADMIN is a protected/read-only core role.
                 */
                addMissingPermissions(superAdminRole, allPrivileges);

                /*
                 * ADMIN default permissions.
                 *
                 * Because existing roles are not overwritten, changing this only affects
                 * fresh databases where ADMIN does not exist yet.
                 */
                createRoleWithDefaultPermissions("ADMIN", Set.of(
                                createStaff,
                                viewCatergories,
                                viewCategoryById,
                                viewPendingItems, 
                                viewCategoryCount,            
                                viewSubCategory,
                                viewItemsCount,
                                viewAvailbleItemsCount, 
                                viewAllItems,
                                viewItemById,
                                createItem,
                                updateItem, 
                                rejectPendingItem, 
                                approvePendingItem,
                                toggleItemAvailability, 
                                deleteItem, 
                                viewAllSubcategories,
                                createQrcode,
                                regenerateQrcode,
                                revokeQrcode,
                                createRestaurantTable,
                                deleteRestaurantTable,
                                updateRestaurantTable,
                                viewRestaurantTable,
                                viewRestaurantTableById
                        ));

                createRoleWithDefaultPermissions("MANAGER", Set.of(
                                viewBranch,
                                manageOrders,
                                viewReports,
                                viewCustomer));

                createRoleWithDefaultPermissions("CHEF", Set.of(
                                manageMenu,
                                updateOrderStatus,
                                viewOrders));

                createRoleWithDefaultPermissions("RECEPTIONIST", Set.of(
                                createOrders,
                                manageReservations,
                                viewCustomer));

                createRoleWithDefaultPermissions("DELIVERY", Set.of(
                                updateDeliveryStatus,
                                viewDelivery));

                createRoleWithDefaultPermissions("CUSTOMER", Set.of(
                                viewOwnOrders,
                                viewOwnProfile));
        }

        /*
         * Creates a privilege only if it does not already exist.
         *
         * This is safe to run every backend startup.
         */
        private Privilege createPrivilege(String name) {
                return privilegeRepository.findByName(name).orElseGet(() -> {
                        Privilege privilege = Privilege.builder()
                                        .name(name)
                                        .build();

                        return privilegeRepository.save(privilege);
                });
        }

        /*
         * Creates a role with default permissions only if the role does not already
         * exist.
         *
         * If the role already exists, we return it as it is.
         * We do NOT overwrite its permissions.
         *
         * This is important because role permissions are editable from the frontend.
         */
        private Role createRoleWithDefaultPermissions(String name, Set<Privilege> defaultPermissions) {
                Role existingRole = roleRepository.findByName(name).orElse(null);

                if (existingRole != null) {
                        return existingRole;
                }

                Role role = Role.builder()
                                .name(name)
                                .permissions(new HashSet<>(defaultPermissions))
                                .build();

                return roleRepository.save(role);
        }

        /*
         * Adds missing permissions without removing existing permissions.
         *
         * We use this only for SUPER_ADMIN so that the system owner role always
         * receives new system privileges added later.
         */
        private void addMissingPermissions(Role role, Set<Privilege> permissionsToAdd) {
                if (role == null) {
                        return;
                }

                Set<Privilege> currentPermissions = role.getPermissions();

                if (currentPermissions == null) {
                        currentPermissions = new HashSet<>();
                }

                boolean changed = currentPermissions.addAll(permissionsToAdd);

                if (changed) {
                        role.setPermissions(currentPermissions);
                        roleRepository.save(role);
                }
        }
}
