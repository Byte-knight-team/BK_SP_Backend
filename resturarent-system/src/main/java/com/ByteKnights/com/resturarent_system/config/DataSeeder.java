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
                Privilege viewAllSubcategories = createPrivilege("VIEW_ALL_SUBCATEGORIES");

                Privilege createCoupon = createPrivilege("CREATE_COUPON");
                Privilege viewCoupons = createPrivilege("VIEW_COUPONS");
                Privilege viewCoupon = createPrivilege("VIEW_COUPON");
                Privilege updateCoupon = createPrivilege("UPDATE_COUPON");
                Privilege updateCouponStatus = createPrivilege("UPDATE_COUPON_STATUS");

                Privilege saveIngredients = createPrivilege("SAVE_INGREDIENTS");
                Privilege viewIngredients = createPrivilege("VIEW_INGREDIENTS");

                Privilege createMenuItemRequest = createPrivilege("CREATE_MENU_ITEM_REQUEST");
                Privilege viewMenuItemRequests = createPrivilege("VIEW_MENU_ITEM_REQUESTS");
                Privilege decideMenuItemRequest = createPrivilege("DECIDE_MENU_ITEM_REQUEST");

                // Delivery
                Privilege updateDeliveryStatus = createPrivilege("UPDATE_DELIVERY_STATUS");
                Privilege viewDelivery = createPrivilege("VIEW_DELIVERY");
                Privilege manageDeliveryStatus = createPrivilege("MANAGE_DELIVERY_STATUS");

                // Branch / reports / profile
                Privilege viewBranch = createPrivilege("VIEW_BRANCH");
                Privilege viewReports = createPrivilege("VIEW_REPORTS");
                Privilege viewOwnProfile = createPrivilege("VIEW_OWN_PROFILE");

                // Manager Dashboard & Modules / Procurement
                Privilege viewDashboard = createPrivilege("VIEW_DASHBOARD");
                Privilege viewAnalytics = createPrivilege("VIEW_ANALYTICS");
                Privilege viewSales = createPrivilege("VIEW_SALES");
                Privilege viewStaff = createPrivilege("VIEW_STAFF");
                Privilege manageDrivers = createPrivilege("MANAGE_DRIVERS");

                Privilege procurementView = createPrivilege("PROCUREMENT_VIEW");
                Privilege procurementManageVendors = createPrivilege("PROCUREMENT_MANAGE_VENDORS");
                Privilege procurementManagePo = createPrivilege("PROCUREMENT_MANAGE_PO");
                Privilege procurementManageGrn = createPrivilege("PROCUREMENT_MANAGE_GRN");

                // QR permissions
                Privilege createQrcode = createPrivilege("CREATE_QR_CODE");
                Privilege regenerateQrcode = createPrivilege("REGENERATE_QR_CODE");
                Privilege revokeQrcode = createPrivilege("REVOKE_QR_CODE");
                Privilege viewActiveQrCode = createPrivilege("VIEW_ACTIVE_QR_CODE");
                Privilege downloadQrCode = createPrivilege("DOWNLOAD_QR_CODE");

                // Restaurant table permissions
                Privilege createRestaurantTable = createPrivilege("CREATE_TABLE");
                Privilege viewRestaurantTable = createPrivilege("VIEW_TABLE");
                Privilege viewRestaurantTableById = createPrivilege("VIEW_TABLE_BY_ID");
                Privilege updateRestaurantTable = createPrivilege("UPDATE_TABLE");

                // Kitchen permissions
                Privilege kitchenViewStats = createPrivilege("KITCHEN_VIEW_STATS");
                Privilege kitchenOrderView = createPrivilege("KITCHEN_ORDER_VIEW");
                Privilege kitchenOrderUpdate = createPrivilege("KITCHEN_ORDER_UPDATE");
                Privilege kitchenOrderAssign = createPrivilege("KITCHEN_ORDER_ASSIGN");
                Privilege kitchenInventoryView = createPrivilege("KITCHEN_INVENTORY_VIEW");
                Privilege kitchenInventoryRequest = createPrivilege("KITCHEN_INVENTORY_REQUEST");
                Privilege kitchenInventoryUpdate = createPrivilege("KITCHEN_INVENTORY_UPDATE");
                Privilege kitchenChefManage = createPrivilege("KITCHEN_CHEF_MANAGE");
                Privilege kitchenAlertCreate = createPrivilege("KITCHEN_ALERT_CREATE");
                Privilege kitchenAlertView = createPrivilege("KITCHEN_ALERT_VIEW");
                Privilege kitchenAlertResolve = createPrivilege("KITCHEN_ALERT_RESOLVE");

                // Line Chef
                Privilege lineChefOrderView = createPrivilege("LINE_CHEF_ORDER_VIEW");
                Privilege lineChefOrderUpdate = createPrivilege("LINE_CHEF_ORDER_UPDATE");

                // RECEPTIONIST
                Privilege receptionistTableView = createPrivilege("RECEPTIONIST_TABLE_VIEW");
                Privilege receptionistTableUpdate = createPrivilege("RECEPTIONIST_TABLE_UPDATE");
                Privilege receptionistReservationCreate = createPrivilege("RECEPTIONIST_RESERVATION_CREATE");
                Privilege receptionistReservationUpdate = createPrivilege("RECEPTIONIST_RESERVATION_UPDATE");
                Privilege receptionistOrderView = createPrivilege("RECEPTIONIST_ORDER_VIEW");
                Privilege receptionistOrderUpdate = createPrivilege("RECEPTIONIST_ORDER_UPDATE");
                Privilege receptionistPaymentCollect = createPrivilege("RECEPTIONIST_PAYMENT_COLLECT");

                // ADMIN
                Privilege viewSummary = createPrivilege("VIEW_SUMMARY");
                Privilege viewOrderFlow = createPrivilege("VIEW_ORDER_FLOW");
                Privilege viewRevenueTrend = createPrivilege("VIEW_REVENUE_TREND");
                Privilege viewBranchRevenue = createPrivilege("VIEW_BRANCH_REVENUE");

                /*
                 * All known system privileges.
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
                                manageDeliveryStatus,
                                viewBranch,
                                viewReports,
                                viewOwnProfile,
                                viewDashboard,
                                viewAnalytics,
                                viewSales,
                                viewStaff,
                                manageDrivers,
                                createQrcode,
                                regenerateQrcode,
                                revokeQrcode,
                                viewActiveQrCode,
                                downloadQrCode,
                                createRestaurantTable,
                                viewRestaurantTable,
                                viewRestaurantTableById,
                                updateRestaurantTable,
                                viewCatergories,
                                viewCategoryById,
                                createCategory,
                                updateCategory,
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
                                viewAllSubcategories,
                                kitchenViewStats,
                                kitchenOrderView,
                                kitchenOrderUpdate,
                                kitchenOrderAssign,
                                kitchenInventoryView,
                                kitchenInventoryRequest,
                                kitchenInventoryUpdate,
                                kitchenChefManage,
                                kitchenAlertCreate,
                                kitchenAlertView,
                                kitchenAlertResolve,
                                receptionistTableView,
                                receptionistTableUpdate,
                                receptionistReservationCreate,
                                receptionistReservationUpdate,
                                receptionistOrderView,
                                receptionistOrderUpdate,
                                receptionistPaymentCollect,
                                lineChefOrderView,
                                lineChefOrderUpdate,
                                procurementView,
                                procurementManageVendors,
                                procurementManagePo,
                                procurementManageGrn,
                                createCoupon,
                                viewCoupons,
                                viewCoupon,
                                updateCoupon,
                                updateCouponStatus,
                                viewSummary,
                                viewOrderFlow,
                                viewRevenueTrend,
                                viewBranchRevenue,
                                saveIngredients,
                                viewIngredients,
                                createMenuItemRequest,
                                viewMenuItemRequests,
                                decideMenuItemRequest);

                /*
                 * For normal roles, default permissions are added ONLY when the role is first
                 * created.
                 * This prevents the DataSeeder from overwriting changes made from the Roles &
                 * Permissions page.
                 */
                Role superAdminRole = createRoleWithDefaultPermissions("SUPER_ADMIN", allPrivileges);

                /*
                 * SUPER_ADMIN is the owner role, gets all currently known system privileges
                 */
                addMissingPermissions(superAdminRole, allPrivileges);

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
                                viewAllSubcategories,
                                createQrcode,
                                regenerateQrcode,
                                revokeQrcode,
                                viewActiveQrCode,
                                downloadQrCode,
                                createRestaurantTable,
                                updateRestaurantTable,
                                viewRestaurantTable,
                                viewRestaurantTableById,
                                viewSummary,
                                viewOrderFlow,
                                viewRevenueTrend,
                                viewBranchRevenue,
                                saveIngredients,
                                viewIngredients,
                                viewMenuItemRequests,
                                decideMenuItemRequest));

                createRoleWithDefaultPermissions("MANAGER", Set.of(
                                viewBranch,
                                manageOrders,
                                viewReports,
                                viewCustomer,
                                viewDashboard,
                                viewAnalytics,
                                viewSales,
                                viewStaff,
                                manageDrivers,
                                procurementView,
                                procurementManageVendors,
                                procurementManagePo,
                                procurementManageGrn));

                createRoleWithDefaultPermissions("CHEF", Set.of(
                                viewCatergories,
                                manageMenu,
                                kitchenViewStats,
                                kitchenOrderView,
                                kitchenOrderUpdate,
                                kitchenOrderAssign,
                                kitchenInventoryView,
                                kitchenInventoryRequest,
                                kitchenInventoryUpdate,
                                kitchenChefManage,
                                kitchenAlertCreate,
                                kitchenAlertView,
                                kitchenAlertResolve,
                                saveIngredients,
                                viewIngredients,
                                createMenuItemRequest));

                createRoleWithDefaultPermissions("RECEPTIONIST", Set.of(
                                viewCustomer,
                                receptionistTableView,
                                receptionistTableUpdate,
                                receptionistReservationCreate,
                                receptionistReservationUpdate,
                                receptionistOrderView,
                                receptionistOrderUpdate,
                                receptionistPaymentCollect,
                                kitchenAlertView));

                createRoleWithDefaultPermissions("DELIVERY", Set.of(
                                updateDeliveryStatus,
                                viewDelivery,
                                manageDeliveryStatus));

                Role lineChefRole = createRoleWithDefaultPermissions("LINE_CHEF", Set.of(
                                lineChefOrderView,
                                lineChefOrderUpdate));
                addMissingPermissions(lineChefRole, Set.of(lineChefOrderView, lineChefOrderUpdate));

                createRoleWithDefaultPermissions("CUSTOMER", Set.of(
                                viewOwnOrders,
                                viewOwnProfile));
        }

        /*
         * Creates a privilege only if it does not already exist.
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
