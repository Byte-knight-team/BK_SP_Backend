package com.ByteKnights.com.resturarent_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
        Role name examples:
        SUPER_ADMIN, ADMIN, MANAGER, CHEF, RECEPTIONIST, DELIVERY
    */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    /*
        Default salary for this role.

        Example:
        RECEPTIONIST role can have baseSalary = 60000.
        When a new RECEPTIONIST staff member is created,
        this value can be copied into Staff.salary.

        This is not payroll.
        This is only the default salary reference for the role.
    */
    @Builder.Default
    @Column(name = "base_salary", precision = 10, scale = 2)
    private BigDecimal baseSalary = BigDecimal.ZERO;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private Set<Privilege> permissions = new HashSet<>();
}