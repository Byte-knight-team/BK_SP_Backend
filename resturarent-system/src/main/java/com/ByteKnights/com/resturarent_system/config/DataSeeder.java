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

@Component
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;

    public DataSeeder(RoleRepository roleRepository, PrivilegeRepository privilegeRepository) {
        this.roleRepository = roleRepository;
        this.privilegeRepository = privilegeRepository;
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
}